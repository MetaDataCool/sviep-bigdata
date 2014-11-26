(ns sviepbd.utils.scraping "DOM querying utilities. Wraps the JSoup library."
  (:import [com.googlecode.htmlcompressor.compressor HtmlCompressor] 
           [java.text NumberFormat] 
           [java.util Locale]
           [org.jsoup Jsoup]
           [org.jsoup.nodes Element Document TextNode]
           [org.jsoup.select Elements]
           )
  (:use clojure.repl clojure.pprint))

(defn ^Element parse-html [html] (Jsoup/parse html))

(defn select [^Element element query]
  (-> element (.select query) seq))
(def select-one (comp first select))

(defn ^Element ancestor-with-tag "Finds the closest ancestor of an element (including itself) that has the given tag" 
  [tag,^Element e]
  (->> (.parents e) seq (cons e) (filter (fn [^Element p] (= (.. p tag getName) tag))) first))

(def td-ancestor (partial ancestor-with-tag "td"))

(defn ^Element next-sibling [^Element e] (.nextElementSibling e))

(defn ^String own-text [^Element element] (.ownText element))
(defn ^String text [^Element element] (.text element))

(defn ^Element find-having-text [^Element root,query,txt]
  (->> (select root query) (filter #(= (text %) txt)) first))

(defn attr [attr-name,^Element e] (.attr e attr-name))

(defn html [^Element e] (.html e))

(def minify-html (let [compressor (doto (HtmlCompressor.) 
                                    (.setCompressCss true)
                                    ;(.setCompressJavaScript true)
                                    )] 
                   #(.compress compressor %)))
