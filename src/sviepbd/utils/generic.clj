(ns sviepbd.utils.generic "generic utilities"
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as log]
            )
  (:use clojure.repl clojure.pprint))

(def errors "Atom that holds the erros encountered by failsafe-map and failsafe-pmap in a vector" 
  (atom []))

(defn failsafe-map [f coll]
  (->> coll (map #(try (f %) (catch Exception e 
                               (do 
                                 (log/warn (str "Problem here : " e))
                                 (swap! errors conj {:error e :item %})
                                 nil))))
    (filter some?)))
(defn failsafe-pmap [f coll]
  (->> coll (pmap #(try (f %) (catch Exception e 
                                (do 
                                  (log/warn (str "Problem here : " e))
                                  (swap! errors conj {:error e :item %})
                                  nil))))
    (filter some?)))

(defn progress-logging-seq "Transforms a seq into a lazy seq with the same elements, but that logs its progression as it gets evaluated."
  ([step seq] (map (fn [i item] (when (-> i (mod step) (= 0)) 
                                  (log/debug (str "PROGRESS : ITEM " i))) 
                     item) 
                   (range) seq))
  ([seq] (progress-logging-seq 1 seq))
  )

(defn seq-of-chan "Creates a lazy seq from a core.async channel." [c]
  (lazy-seq
    (let [fst (a/<!! c)]
      (if (nil? fst) nil (cons fst (seq-of-chan c)) ))))

(defn map-pipeline-async "Map for asynchronous functions, backed by clojure.core.async/pipeline-async .

From an asynchronous function af, and a seq coll, creates a lazy seq that is the result of applying the asynchronous function af to each element of coll.
af must be an asyncronous function as described in clojure.core.async/pipeline-async.
takes an optional p parallelism number.

Note that unlike pmap, map-pipeline-async will keep the order of the original sequence."
  ([af p coll]
    (let [ic (a/chan p), oc (a/chan p)]
      (a/onto-chan ic coll)
      (a/pipeline-async p oc af ic)
      (seq-of-chan oc)
      ))
  ([af coll] (map-pipeline-async af 200 coll)))

(defn >!!-and-close! "Puts a value into a channel if its not nil, then closes the channel."
  [port v]
  (when (some? v) (a/>!! port v))
  (a/close! port))
