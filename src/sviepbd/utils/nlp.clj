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
;; Tokenization
;; ----------------------------------------------------------------

(def stopword? #{"a" "about" "above" "after" "again" "against" "all" "am" "an" "and" "any" "are" "aren't" "as" "at" "be" "because" "been" "before" "being" "below" "between" "both" "but" "by" "can't" "cannot" "could" "couldn't" "did" "didn't" "do" "does" "doesn't" "doing" "don't" "down" "during" "each" "few" "for" "from" "further" "had" "hadn't" "has" "hasn't" "have" "haven't" "having" "he" "he'd" "he'll" "he's" "her" "here" "here's" "hers" "herself" "him" "himself" "his" "how" "how's" "i" "i'd" "i'll" "i'm" "i've" "if" "in" "into" "is" "isn't" "it" "it's" "its" "itself" "let's" "me" "more" "most" "mustn't" "my" "myself" "no" "nor" "not" "of" "off" "on" "once" "only" "or" "other" "ought" "our" "ours" "ourselves" "out" "over" "own" "same" "shan't" "she" "she'd" "she'll" "she's" "should" "shouldn't" "so" "some" "such" "than" "that" "that's" "the" "their" "theirs" "them" "themselves" "then" "there" "there's" "these" "they" "they'd" "they'll" "they're" "they've" "this" "those" "through" "to" "too" "under" "until" "up" "very" "was" "wasn't" "we" "we'd" "we'll" "we're" "we've" "were" "weren't" "what" "what's" "when" "when's" "where" "where's" "which" "while" "who" "who's" "whom" "why" "why's" "with" "won't" "would" "wouldn't" "you" "you'd" "you'll" "you're" "you've" "your" "yours" "yourself" "yourselves"})
(defn stopword-token? [{:keys [lemma]}] (stopword? lemma))

(def tokenize-pipeline "CoreNLP pipeline for tokenization"
  (StanfordCoreNLP. (doto (java.util.Properties.) 
                      (.put "annotators" (s/join ", " ["tokenize" "ssplit" "pos" "lemma"])) )))


(defn- token-as-map [^Annotation token]
  {:word (get-text-ann token), :pos (get-pos-ann token), :lemma (get-lemma-ann token)})

(defn lowercase-lemma [{:keys [lemma] :as token}]
  (assoc token :lemma (s/lower-case lemma)))

(defn tokenize "Tokenizes a text. The result is a sequence of maps with :word, :pos (part-of-speech) and :lemma (lemmatization) keys."
  [^String text]
  (->> (doto (Annotation. text) (annotate! tokenize-pipeline))
    get-tokens-ann seq
    (map token-as-map)
    ))

(def non-word-pos "A set of Part-Of-Speech tags that are not words."
  #{"$" "``" "''" "(" ")" "," "--" "." ":" "SYM" "|"})

(defn non-word-token? "Checks is the argument is a non-word token." 
  [{:keys [pos] :as token}] (non-word-pos pos))

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

