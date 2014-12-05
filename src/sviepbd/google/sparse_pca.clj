(ns sviepbd.google.sparse-pca
  (:import [java.util TreeSet ArrayList Comparator]
           [org.la4j LinearAlgebra]
           [org.la4j.matrix Matrix Matrices]
           [org.la4j.matrix.functor MatrixProcedure]
           [org.la4j.matrix.source MatrixSource]
           [org.la4j.matrix.sparse CRSMatrix CCSMatrix SparseMatrix]
           [org.la4j.factory Factory CRSFactory CCSFactory]
           [org.la4j.vector Vector]
           [org.la4j.vector.functor VectorProcedure]
           [org.la4j.vector.sparse CompressedVector SparseVector]
           )
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.core.matrix :as m]
            [clatrix.core :as cl]
            [taoensso.timbre :as log])
  (:use clojure.repl clojure.pprint))

(set! *warn-on-reflection* true)
(require '[taoensso.timbre.profiling :as pr])

;; ----------------------------------------------------------------
;; Vector & Matrix utilities - wrappers for la4j
;; ----------------------------------------------------------------

(def ^CRSFactory crs-factory LinearAlgebra/CRS_FACTORY)
(def ^CCSFactory ccs-factory LinearAlgebra/CCS_FACTORY)
(def ^Factory default-factory LinearAlgebra/DEFAULT_FACTORY)

(defn ^Vector create-vector "Creates a vector of length n with the specified Factory." 
  [^Factory fact, n] (.createVector fact (int n)))
(defn ^Matrix create-matrix "Creates a matrix of size m x n with the specified Factory."
  [^Factory fact, m n] (.createMatrix fact (int m) (int n)))

(defn height "number of rows of matrix m" [^Matrix m] (.rows m))
(defn width "number of columns of matrix m" [^Matrix m] (.columns m))
(defn dimensions [m] [(height m) (width m)])
(defn length "dimension of a vector" [^Vector v] (.length v))

(defn get-v [^Vector v, i] (.get v i))
(defn get-m [^Matrix m, i j] (.get m i j))

(defn set-v! [^Vector v, i, val] (.set v i (double val)))
(defn set-m! [^Matrix m, i j val] (.set m i j (double val)))

(defn ^Vector get-column "Returns a copy of the j-th column of m as a Vector." 
  [^Matrix m, j] (.getColumn m j))

(defmacro v-proc "Creates an instance of VectorProcedure with an fn-like syntax." 
  [[i value] & body]
  `(reify VectorProcedure (apply [this# ~i ~value] ~@body)))
(defmacro m-proc "Creates an instance of MatriceProcedure with an fn-like syntax"
  [[i j value] & body]
  `(reify MatrixProcedure (apply [this# ~i ~j ~value] ~@body)))

(defmacro each-non-zero-v! "Iterates over the i-v_i pairs of Vector v for which v_i is non-zero; creates a VectorProcedure under the hood."
  [v [i value :as bindings] & body] `(.eachNonZero ^Vector ~v (v-proc ~bindings ~@body)))
(defmacro each-non-zero-m! "Iterates over the i-j-m_ij tuples of Matrix m for which m_ij is non-zero; creates a MatrixProcedure under the hood." 
  [m [i j value :as args] & body] `(.eachNonZero ^Matrix ~m (m-proc ~args ~@body)))


(defn <v|v> "Inner product with good performance characteristics on sparse vectors (assumes the left vector is sparse)."
  [^Vector v1, ^Vector v2]
  (with-local-vars [ret 0]
    (each-non-zero-v! v1 [i value] (var-set ret (+ (* value (.get v2 i)) 
                                                   (var-get ret))))
    (var-get ret)
    ))

(defn transp-sm "Transpose operator for (very) sparse matrices." 
  ([^Matrix m, ^Factory f]
    (let [tm (.createMatrix f (int (width m)) (int (height m)))]
      (each-non-zero-m! m [i j value] (.set tm j i value))
      tm))
  ([^Matrix m] (transp-sm m (.factory m)))
  )

(defn compressed-vector? [^Vector v] (instance? CompressedVector v))
(defn cardinality "The number of non-zero coordinates of v." 
  [^Vector v] (if (compressed-vector? v)
                (.cardinality ^CompressedVector v)
                (with-local-vars [count 0]
                  (each-non-zero-v! v [_ _] (var-set count (-> count var-get inc)))
                  (var-get count))
                ))

(defn support-indices "Returns a sequence of the non zero indices of v" 
  [^Vector v]
  (let [support-list (ArrayList. (int (if (compressed-vector? v) (cardinality v) 0)))]
    (each-non-zero-v! v [i _] (.add support-list i))
    (seq support-list)
    ))

(defn vec-as-arrays-seq "Represents a vector as a list of [i vi] where vi is non-zero" 
  [v] (->> v support-indices (map (fn [i] [i (get-v v i)]))))

(defn mat-as-arrays-seq "Represents a matrix as a list of [i j mij] where mij is non-zero" 
  [^Matrix mat] (let [sup-ijs (ArrayList.)]
                  (each-non-zero-m! mat [i j value] (.add sup-ijs [i j value]))
                  (seq sup-ijs)))

(defn arrays-seq-to-matrix 
  ([factory m n arrays] (let [m (create-matrix factory m n)]
                          (doseq [[i j mij] arrays] (set-m! m i j mij))
                          m)))
(defn arrays-seq-to-vector 
  ([factory n arrays] (let [v (create-vector factory n)]
                        (doseq [[i vi] arrays] (set-v! v i vi))
                        v)))

(defn ^Matrix from-square-vec
  ([fact sarr] (let [m (count sarr), n (count (first sarr))
                     ret (create-matrix fact m n)]
                 (doseq [i (range m) j (range n)] (set-m! ret i j (get-in sarr [i j])))
                 ret))
  ([sarr] (from-square-vec default-factory sarr)))
(defn ^Vector from-vec
  ([fact v] (let [n (count v), ret (create-vector fact n)]
              (doseq [i (range n)] (set-v! ret i (v i)))
              ret))
  ([v] (from-vec default-factory v)))

(defn ^Vector *mv "default implementation of matrix-vector mutiplication" 
  [^Matrix m, ^Vector v] (.multiply m v))
(defn ^Matrix *mm "default implementation of matrix-matrix mutiplication"
  [^Matrix m1, ^Matrix m2] (.multiply m1 m2))

(defn r*sv "Scalar multiplication adapted to sparse vectors" 
  [r, ^Vector v] (let [ret (.copy v)]
                   (each-non-zero-v! v [i vi] (.set ret i (* (double vi) (double r))))
                   ret))

(defn ^Vector m*sv "Fast matrix-vector multiplication, assuming the vector is sparse."
  [^Matrix m, ^Vector v]
  (let [fact (.. m factory)
        ^Vector ret (.createVector fact (int (height m)))]
    (each-non-zero-v! 
      v [j vj]
      (.eachNonZeroInColumn m j
        (m-proc [i _ mij] (let [vi*mij (* vj mij)] (when (> vi*mij 0) (.set ret i (+ (.get ret i) vi*mij)))))))
    ret))

(defn +mm [^Matrix m1, ^Matrix m2] (.add m1 m2))
(defn subtract [^Matrix m1, ^Matrix m2] (.subtract m1 m2))

(defn to-row-matrix [^Vector v] (.toRowMatrix v))
(defn to-column-matrix [^Vector v] (.toColumnMatrix v))

(defn ^Matrix transpose "Default implementation of matrix transposition, takes an optional Factory."
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

(defn transpose-ccs "Faster transposition using creating a MatrixSource proxy" 
  [^Matrix m]
  (let [f (.factory m)
        source (reify MatrixSource
                 (columns [_] (.rows m))
                 (rows [_] (.columns m))
                 (get [_ i j] (.get m j i))
                 )]
    (.createMatrix f source)))

;; ----------------------------------------------------------------
;; Sparse Power-Iteration
;; ----------------------------------------------------------------

(defn ^Vector project-unit-circle "Scales v to have a euclidian norm of 1.0" 
  [^Vector v] (let [n (|sv| v)]
                (if (> n 0) 
                  (r*sv (/ 1.0 n) v)
                  (throw (IllegalArgumentException. "Can't project zero vector on unit circle")))))

(def threshold "returns a 'copy' of Vector v for which all but the k largest components of v are zeroed." 
  (let [critical-card 100
        get-abs (fn [i-vi] (->> (get i-vi 1) double java.lang.Math/abs))
        i-vi-comp (reify Comparator ;; compares [i v_i] pair by ascending value, then descending index
                    (compare [this p1 p2]
                      (let [[i1 v1] p1, [i2 v2] p2
                            comp1 (java.lang.Double/compare (java.lang.Math/abs (double v1)) 
                                                            (java.lang.Math/abs (double v2)))]
                        (if (zero? comp1) (java.lang.Integer/compare i2 i1) comp1)
                        )))]
    (fn threshold [k, ^Vector v]
     (let [si (support-indices v)
           c (if (compressed-vector? v) (cardinality v) (count si))
           
           largest-i-vis (cond
                           (< c k) (vec-as-arrays-seq v)
                           (< c critical-card) (->> v vec-as-arrays-seq (sort-by get-abs) (take-last k))
                           :else ;; iterate over the seq of non-zero indices, keeping the k highest in a SortedSet 
                           (let [t (TreeSet. i-vi-comp)]
                             (each-non-zero-v! v [i vi] 
                                               (.add t [i vi])
                                               (when (-> t .size (> k)) (.pollFirst t)))
                             (->> t seq))
                           )]
       (let [ret (.blank v ccs-factory)]
         (doseq [[i vi] largest-i-vis] (.set ret i vi))
         ret)))))

(defn- pick-init-vector [^Matrix m]
  (let [ret (.createVector ccs-factory (int (width m)))]
    (each-non-zero-m! m [i j _] (set-v! ret j 1.0))
    ret))

(defn power-iteration "Performs a Power iteration of the provided matrix. `p` denotes a vector a features, `q` a vector of points.
The options are :

:init-vector : the vector of features (p) on which to start the iteration. 
Defaults to having 1.0 on every feature for which the matrix has a non-zero column.

:p-threshold : an integer, the number of largest components of p to keep at the end of an iteration.

:q-threshold : an integer, the number of largest components of q to keep at the end of an iteration.

:stop-epsilon : a small positive real number, the distance of 2 subsequent values of p or q under which the algorithm should stop."
  [m {:keys [init-vector stop-epsilon p-threshold q-threshold] 
      :or {init-vector (pr/p :pick-inivector (pick-init-vector m))
           stop-epsilon 1e-6}}]
  (let [tm (pr/p :transpose-m (#_transpose transpose-ccs m))] 
    (loop [p init-vector
           q (->> p (m*sv m) (pr/p :m*p) (threshold q-threshold) (pr/p :q-total))
           rem (range)]
      (let [p1 (->> q (m*sv tm) (pr/p :tm*q))
            p2 (->> p1 (threshold p-threshold) (pr/p :threshold-p))
            next-p (->> p2 project-unit-circle (pr/p :project-p)) #_(->> q (m*sv tm) (threshold p-threshold) project-unit-circle)
            q1 (->> next-p (m*sv m) (pr/p :m*p))
            q2 (->> q1 (threshold q-threshold) (pr/p :threshold-q))
            next-q (->> q2 project-unit-circle (pr/p :project-q)) #_(->> next-p (m*sv m) (pr/p :m*p) (threshold q-threshold) (pr/p :q-total))
            step (first rem)]
        ;(log/debug (str "STEP " step))
        (if (and (< (euclid-distance next-p p) stop-epsilon)
                 (< (euclid-distance next-q q) (* (|sv| q) stop-epsilon)))
          {:p p, :q q, :step step}
          (recur next-p next-q (next rem))
          ))
      ))
  )

(defn- m+sm [^Matrix m1, ^Matrix m2]
  (let [d (.copy m1)]
    (each-non-zero-m! m2 [i j m2ij] (.set d i j (+ (double (.get d i j)) (double m2ij))))
    d))

(defn- m-sm [^Matrix m1, ^Matrix m2]
  (let [d (.copy m1)]
    (each-non-zero-m! m2 [i j m2ij] (.set d i j (- (double (.get d i j)) (double m2ij))))
    d))

(defn- sv-o*-sv [^Vector v1, ^Vector v2]
  (let [^Matrix ret (create-matrix (.factory v1) (length v1) (length v2))]
    (each-non-zero-v! 
      v1 [i v1i]
      (each-non-zero-v!
        v2 [j v2j]
        (.set ret (int i) (int j) (* (double v1i) (double v2j)))))
    ret))

(defn get-sparse-components [m {:keys [iterations 
                                       init-vector stop-epsilon p-threshold q-threshold] :as config}]
  (pr/p :get-sparse-component-total 
    (let [norm-m (frobenius-norm m)] 
     (->> (range iterations)
                           (reductions 
                             (fn [{:keys [rem approx-m initial-matrix] :as previous} i]
                               (log/debug (str "Iteration : " i))
                               (let [{:keys [p q] :as pi-res} (pr/p :power-itr (power-iteration rem config))
                                     component (pr/p :op1  (sv-o*-sv q p) #_(*mm (to-column-matrix q) (to-row-matrix p)))
                                     new-approx (pr/p :op2 (m+sm #_+mm approx-m component))
                                     new-rem (pr/p :op3 (m-sm #_subtract rem component))]
                                 (-> previous 
                                   (merge pi-res) 
                                   (assoc :rem new-rem
                                          :component component
                                          :approx-m new-approx
                                          :relative-error (pr/p :op4 (/ (frobenius-norm new-rem) norm-m)))
                                   )))
                             {:initial-matrix m :rem m, :approx-m (create-matrix ccs-factory (height m) (width m))})
                           doall))))

;; ----------------------------------------------------------------
;; Loading data
;; ----------------------------------------------------------------

(defn load-sparse-matrix "Loads a sparse matrix of specified dimensions from source. 
:normalization can be :none or :boolean" 
  [source m n {:keys [csv-sep factory normalization]
               :or {csv-sep \space
                    factory ccs-factory
                    normalization :boolean ;; can also be :none
                    }}]
  (let [m (create-matrix factory m n)]
           (with-open [rdr (io/reader source)]
             (doseq [[i j val] (csv/read-csv rdr :separator csv-sep)] 
               (set-m! m 
                       (java.lang.Integer/parseInt i) 
                       (java.lang.Integer/parseInt j)
                       (case normalization
                         :boolean 1.0 
                         :none (java.lang.Double/parseDouble val))
                       ))
             m)))

(defn load-words-map "Loads the source into a id-word map" 
  [source {:keys [csv-sep] :or {csv-sep \space}}]
  (with-open [rdr (io/reader source)]
      (->> (csv/read-csv rdr :separator \space)
        (map (fn [[j word]] {:j (java.lang.Integer/parseInt j) :word word}))
        (reduce (fn [r {:keys [word j]}] (assoc! r j word)) (transient {})) persistent!
        )))

(defn sum-rows [^Matrix m]
  (let [ret (create-vector (.factory m) (width m))]
    (each-non-zero-m! m [_ j mij] (set-v! ret j (+ (get-v ret j) mij)))
    ret))

(defn truncate-low-features [keep-n,^Matrix m]
  (let [kept-indices (->> m sum-rows vec-as-arrays-seq (sort-by second) reverse (take keep-n) (map first))
        ret (.blank m)]
    (doseq [j kept-indices] (.setColumn ret j (.getColumn m j)))
    ret))

(comment 
  (do ;; few results dataset
    (def m (->> (load-sparse-matrix "unversioned/sparse_matrices/few-results_matrix.csv"
                                  421 32615 {}) (truncate-low-features 5000)))
    (def word-of-id (load-words-map "unversioned/sparse_matrices/few-results_words.csv" {})))
  (do ;; many results dataset
    (def m (load-sparse-matrix "unversioned/sparse_matrices/many-results_matrix.csv"
                              9000 2950 {}))
    (def word-of-id (load-words-map "unversioned/sparse_matrices/many-results_words.csv" {})))
  
  (let [{:keys [p q step error]} 
        (power-iteration m {:p-threshold 50 :q-threshold 5000})]
    (def pf p) (def qf q))
  
  (->> pf vec-as-arrays-seq
    (map #(update-in % [0] word-of-id))
    (sort-by second) reverse
    (take 100)
    pprint)
  
  (def components (get-sparse-components
                    m {:iterations 5
                       :p-threshold 50 :q-threshold 200}))
  )