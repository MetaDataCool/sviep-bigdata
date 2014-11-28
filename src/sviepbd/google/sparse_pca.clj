(ns sviepbd.google.sparse-pca
  (:import [java.util TreeSet ArrayList Comparator] 
           [org.la4j.matrix Matrix Matrices]
           [org.la4j.matrix.functor MatrixProcedure]
           [org.la4j.matrix.sparse CRSMatrix CCSMatrix]
           [org.la4j.factory Factory CRSFactory CCSFactory]
           [org.la4j.vector Vector]
           [org.la4j.vector.functor VectorProcedure]
           [org.la4j.vector.sparse CompressedVector]
           )
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.core.matrix :as m]
            [clatrix.core :as cl]
            [taoensso.timbre :as log])
  (:use clojure.repl clojure.pprint))

(comment 
  (set! *warn-on-reflection* true)
  
  (def n 10000)
  (def m 4000)
  
  ;; dont evaluate it in the REPL, it will take forever to print
  (def my-sparse (doto (.createMatrix crs-factory n m)
                   (.set 2 4 1.0)
                   (.set 1 45 0)
                   (.set 32 76 2.1)
                   (.set 4563 4 1.2)
                   ))
  (def t-my-sparse (.transpose my-sparse ccs-factory))
  (def p (doto ^Vector (.createVector crs-factory n)
           (.set 0 2)
           (.set 3 -3.4)
           (.set 4563 2.1)
           (.set 32 1.5)
           (.set 23 -0.3)
           ))
  
  (def q (doto ^Vector (.createVector crs-factory m)
           (.set 0 1)
           (.set 45 1.3)
           (.set 76 98)
           (.set 4 -3)
           (.set 17 1.7)
           (.set 145 -2.1)
           ))
  
  )

;; ----------------------------------------------------------------
;; Vector utilities - wrappers for la4j
;; ----------------------------------------------------------------

(def ^CRSFactory crs-factory (CRSFactory.))
(def ^CCSFactory ccs-factory (CCSFactory.))

(defmacro v-proc "Creates an instance of VectorProcedure with an fn-like syntax." 
  [[i value] & body]
  `(reify VectorProcedure (apply [this# ~i ~value] ~@body)))
(defmacro m-proc "Creates an instance of MatriceProcedure with an fn-like syntax"
  [[i j value] & body]
  `(reify MatrixProcedure (apply [this# ~i ~j ~value] ~@body)))

(defmacro each-non-zero-v! [v [i value :as args] & body]
  `(.eachNonZero ^Vector ~v (v-proc ~args ~@body)))
(defmacro each-non-zero-m! [m [i j value :as args] & body]
  `(.eachNonZero ^Matrix ~m (m-proc ~args ~@body)))


(defn <v|v> "Inner product with good performance characteristics on sparse vectors (assumes the left vector is sparse)."
  [^Vector v1, ^Vector v2]
  (with-local-vars [ret 0]
    (each-non-zero-v! v1 [i value] (var-set ret (+ (* (.get v1 i) (.get v2 i)) 
                                                   (var-get ret))))
    (var-get ret)
    ))

(defn height "number of rows of matrix m" [^Matrix m] (.rows m))
(defn width "number of columns of matrix m" [^Matrix m] (.columns m))

(defn get-v [^Vector v, i] (.get v i))
(defn get-m [^Matrix m, i j] (.get m i j))

(defn transp-sm "Transpose operator for (very) sparse matrices." 
  ([^Matrix m, ^Factory f]
    (let [tm (.createMatrix f (int (width m)) (int (height m)))]
      (each-non-zero-m! m [i j value] (.set tm j i value))
      tm))
  ([^Matrix m] (transp-sm m (.factory m)))
  )

(defn create-vector [^Factory fact, n]
  (.createVector fact (int n)))

(defn set-v! [^Vector v, i, val] (.set v i val))
(defn set-m! [^Matrix m, i j val] (.set m i j val))

(defn ^Vector get-column [^Matrix m, j]
  (.getColumn m j))

(defn compressed-vector? [^Vector v] (instance? CompressedVector v))
(defn cardinality [^CompressedVector v] (.cardinality v))

(defn support-indices "Returns a sequence of the non zero indices of v" 
  [^Vector v]
  (let [support-list (ArrayList. (int (if (compressed-vector? v) (cardinality v) 0)))]
    (each-non-zero-v! v [i _] (.add support-list i))
    (seq support-list)
    ))

(defn sum-vs [vs]
  (if (empty? vs)
    (create-vector ccs-factory 1)
    (let [^Vector v0 (first vs)]
      (reduce (fn [^Vector s, ^Vector v]
                (.add s v)) 
              v0 (rest vs))
      )))

(defn ^Vector *mv [^Matrix m, ^Vector v] (.multiply m v))
(defn ^Matrix *mm [^Matrix m1, ^Matrix m2] (.multiply m1 m2))

(defn ^Vector m*sv "Fast matrix x vector multiplication, assuming the vector is sparse."
  [^Matrix m, ^Vector v]
  (let [fact (.. m factory)
        ^Vector ret (.createVector fact (int (height m)))]
    (each-non-zero-v! 
      v [j vj]
      (.eachNonZeroInColumn m j
        (m-proc [i _ mij] (.set ret i (+ (.get ret i) (* vj mij))))))
    ret))

(defn ^Matrix transpose "Wrapper function for the .transpose() Matrix method."
  ([^Matrix m] (.transpose m))
  ([^Matrix m,^Factory f] (.transpose m f)))

(defn |sv| "Euclidian norm using sparse inner product." 
  [^Vector v] (-> (<v|v> v v) java.lang.Math/sqrt))

(defn euclid-distance "Euclidan distance using sparse inner product"
  [^Vector v1, ^Vector v2] (|sv| (.subtract v1 v2)))

(defn m-each-non-zero! [f!,^Matrix m]
  (.eachNonZero m (reify MatrixProcedure 
                    (apply [this i j v] (f! i j v)))
    ))

(defn frobenius-norm [^Matrix m]
  (->> (mat-as-arrays-seq m) (map #(nth % 2)) (map #(* % %)) (reduce + 0.0) java.lang.Math/sqrt))
(defn frob-distance [^Matrix m1, ^Matrix m2] (frobenius-norm (.subtract m1 m2)))

;; ----------------------------------------------------------------
;; Utils
;; ----------------------------------------------------------------

(defn vec-as-arrays-seq [v]
  (->> v support-indices (map (fn [i] [i (get-v v i)]))))

(defn mat-as-arrays-seq [^Matrix mat]
  (let [sup-ijs (ArrayList.)]
    (each-non-zero-m! mat [i j value] (.add sup-ijs [i j value]))
    (seq sup-ijs)))

;; ----------------------------------------------------------------
;; Sparse Power-Iteration
;; ----------------------------------------------------------------

(defn ^Vector project-unit-circle [^Vector v]
  (let [n (|sv| v)]
    (if (> n 0) 
      (.divide v n)
      (throw (IllegalArgumentException. "Can't project zero vector on unit circle")))))

(def threshold "zeroes all but the k largest components of v" 
  (let [critical-card 100]
    (fn threshold [k, ^Vector v]
     (let [si (support-indices v)
           c (if (compressed-vector? v) (cardinality v) (count si))
           get-abs (fn [i] (->> i (.get v) java.lang.Math/abs))
           largest-is (cond
                        (< c k) si
                        (< c critical-card) (->> si (sort-by get-abs) (take-last k))
                        :else ;; iterate over the seq of non-zero indices, keeping the k highest in a SortedSet 
                        (let [t (TreeSet. (reify Comparator
                                            (compare [this i j]
                                              (let [vi (get-abs i), vj (get-abs j)]
                                                (cond 
                                                  (> vi vj) 1
                                                  (< vi vj) -1
                                                  :else (- j i) ;; smallest index wins when in doubt
                                                  )))
                                            ))]
                          (doseq [i si] 
                            (.add t i)
                            (when (-> t .size (> k)) (.pollFirst t))
                            )
                          (-> t seq))
                        )]
       (let [ret (.blank v crs-factory)]
         (doseq [i largest-is] (.set ret i (.get v i)))
         ret)))))

(defn- pick-init-vector [^Matrix m]
  (let [ret (.createVector crs-factory (.columns m))]
    (each-non-zero-m! m [i j _] (set-v! ret j 1.0))
    ret))

(comment 
  (def steps-data (atom []))
  )

(defn power-iteration 
  [m {:keys [init-vector stop-epsilon p-threshold q-threshold] 
      :or {init-vector (pick-init-vector m)
           stop-epsilon 0.0000000001}}]
  (let [tm (transpose m)] 
    (loop [p init-vector
           q (->> p (m*sv m) (threshold q-threshold) project-unit-circle)
           rem (range)]
      (let [next-p (->> q (m*sv tm) (threshold p-threshold) project-unit-circle)
            next-q (->> next-p (m*sv m) (threshold q-threshold) project-unit-circle)
            step (first rem)]
        (log/debug (str "STEP " step))
        (if (and (< (euclid-distance next-p p) stop-epsilon)
                 (< (euclid-distance next-q q) stop-epsilon))
          {:p next-p, :q next-q, :step step
           :error (/ (frob-distance m (*mm (.toColumnMatrix (m*sv m next-p)) (.toRowMatrix p)))
                     (frobenius-norm m))}
          (recur next-p next-q (next rem))
          ))
      ))
  )

;; ----------------------------------------------------------------
;; Loading data
;; ----------------------------------------------------------------
(comment 
  (def m (let [m (.createMatrix ccs-factory 421 32615)]
           (with-open [rdr (io/reader "unversioned/sparse_matrices/few-results_matrix.csv")]
             (doseq [[i j val] (csv/read-csv rdr :separator \space)] 
               (set-m! m 
                       (java.lang.Integer/parseInt i) 
                       (java.lang.Integer/parseInt j)
                       (java.lang.Double/parseDouble val)))
             m
             )))
  
  (def word-of-id
    (with-open [rdr (io/reader "unversioned/sparse_matrices/few-results_words.csv")]
      (->> (csv/read-csv rdr :separator \space)
        (map (fn [[j word]] {:j (java.lang.Integer/parseInt j) :word word}))
        (reduce (fn [r {:keys [word j]}] (assoc! r j word)) (transient {})) persistent!
        )))
  
  (let [{:keys [p q step error]} (power-iteration m {:p-threshold 50 :q-threshold 50})]
    (def pf p) (def qf q) (def res-error error))
  
  (->> pf vec-as-arrays-seq
    (map #(update-in % [0] word-of-id))
    (sort-by second) reverse
    pprint)
  )