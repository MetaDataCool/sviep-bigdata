(ns sviepbd.utils.nlp "wrappers around Stanford CoreNLP to get more idiomatic and comfortable Clojure."
  (:import [edu.stanford.nlp.pipeline StanfordCoreNLP Annotation Annotator]
           [edu.stanford.nlp.util CoreMap]
           [edu.stanford.nlp.ling CoreLabel
            CoreAnnotations$SentencesAnnotation 
            CoreAnnotations$TokensAnnotation
            CoreAnnotations$TextAnnotation
            CoreAnnotations$PartOfSpeechAnnotation
            CoreAnnotations$NamedEntityTagAnnotation
            CoreAnnotations$LemmaAnnotation
            ]
           )
  (:require [clojure.string :as s])
  (:use clojure.repl clojure.pprint))
;; ----------------------------------------------------------------
;; Annotations classes
;; ----------------------------------------------------------------
(def sentences-ann CoreAnnotations$SentencesAnnotation)
(def tokens-ann CoreAnnotations$TokensAnnotation)
(def text-ann CoreAnnotations$TextAnnotation)
(def pos-ann CoreAnnotations$PartOfSpeechAnnotation)
(def ner-ann CoreAnnotations$NamedEntityTagAnnotation)
(def lemma-ann CoreAnnotations$LemmaAnnotation)

;; ----------------------------------------------------------------
;; Functional wrappers
;; ----------------------------------------------------------------
(defn get-ann [ann-class, ^Annotation ann] (.get ann ann-class))
(defn annotate! [^Annotation annotation, ^Annotator annotator] (.annotate annotator annotation))

(def get-sentences-ann (partial get-ann sentences-ann))
(def get-tokens-ann (partial get-ann tokens-ann))
(def get-text-ann (partial get-ann text-ann))
(def get-pos-ann (partial get-ann pos-ann))
(def get-ner-ann (partial get-ann ner-ann))
(def get-lemma-ann (partial get-ann lemma-ann))

;; ----------------------------------------------------------------
;; Example
;; ----------------------------------------------------------------
(comment
  (do 
    (def pipeline 
     (StanfordCoreNLP. (doto (java.util.Properties.) 
                         (.put "annotators" (s/join ", " ["tokenize" "ssplit" "pos" "lemma"])) )))

    (def text "Depending on which annotators you use, please cite the corresponding papers on: POS tagging, NER, parsing (with parse annotator), dependency parsing (with depparse annotator), coreference resolution, or sentiment.

To construct a Stanford CoreNLP object from a given set of properties, use StanfordCoreNLP(Properties props). This method creates the pipeline using the annotators given in the \"annotators\" property (see above for an example setting). The complete list of accepted annotator names is listed in the first column of the table above. To parse an arbitrary text, use the annotate(Annotation document) method.

The code below shows how to create and use a Stanford CoreNLP object:")

    (def document (doto (Annotation. ^String text) (annotate! pipeline)))
    
    
    (def sentences (seq (get-ann sentences-ann document)))
    (def tokens (->> document
                  get-tokens-ann seq
                  (map (fn [token] {:word (get-text-ann token)
                                    :pos (get-pos-ann token)
                                    :lemma (get-lemma-ann token)}))
                  ))
    ))

