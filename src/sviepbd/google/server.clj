(ns sviepbd.google.server
  (:require [org.httpkit.server :as server]
            [taoensso.timbre :as log]
            
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as r]
            
            [ring.middleware.cookies :as cookies]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kw-params]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            
            [clojure.core.async :as a]
            [org.httpkit.client :as http]
            
            [sviepbd.google.crawler :as crawler]
            
            [sviepbd.utils.generic :as u])
  (:use clojure.repl clojure.pprint))

;; ----------------------------------------------------------------
;; Completion notification logic
;; ----------------------------------------------------------------

(def ^:private completed-scrapings-chan-in (a/chan 32))
(def completed-scrapings-mult (a/mult completed-scrapings-chan-in))

(defn send-completions-to-hooks! []
  (let [hooks (if-let [config (java.lang.System/getenv "ONCOMPLETE_HOOKS")]
                (->> (clojure.string/split config #"[ ,]+") (remove empty?))
                [])
        c (a/chan 32)]
    (a/tap completed-scrapings-mult c)
    (u/godochan [data c]
             (a/<! (a/thread (doseq [url hooks] (http/post url {:body (update-in data [:query_id] str)})) )))
    (log/info (str "Ready to send completions data to " hooks))
    ))
(defn save-completions-to-db! []
  (let [coll-name "completions" ; TODO may want to move this with other collections declarations
        c (a/chan 32)]
    (u/godochan [data c] (-> (monger.collection/insert coll-name data) a/thread a/<!))
    (a/tap completed-scrapings-mult c)
    (log/info (str "Ready to save completions data in collection " coll-name))))

;; ----------------------------------------------------------------
;; Requests handlers
;; ----------------------------------------------------------------

(defn start-scraping! [{:keys [query] :as body}]
  (let [{:keys [query_id completion-chan]} (crawler/perform-scraping! query body)]
    (a/pipe completion-chan completed-scrapings-chan-in false)
    (-> {:query_id (str query_id), :query query} r/response)))

;; ----------------------------------------------------------------
;; Routes declaration
;; ----------------------------------------------------------------

(defroutes app-routes
  (POST "/start-scraping" {:keys [body]} (start-scraping! body))
  (route/not-found "NOT FOUND")
  )

;; ----------------------------------------------------------------
;; Handler definition
;; ----------------------------------------------------------------

(def app 
  (-> #(app-routes %)
    ;; ---- Middleware ----
    ;; requests go from bottom to top, responses from top to bottom
    
    (ring-json/wrap-json-body {:keywords? true})
    ring-json/wrap-json-response
    
    (wrap-defaults api-defaults)
    ))

;; ----------------------------------------------------------------
;; Server
;; ----------------------------------------------------------------

(defn init! []
  (log/info "Connecting to MongoDB...")
  (crawler/connect-mongo!)
  ;(send-completions-to-hooks!)
  (save-completions-to-db!)
  )

(defn start-server! [port] 
  (let [stopper (server/run-server #(app %) {:port port})]
    (log/info (str "Server started on port " port ", GO BEARS!"))
    stopper))

(defn -main
  "I don't do a whole lot ... yet."
  [& [port]]
  (let [port (-> port (or "3000") java.lang.Integer/parseInt)] ;; get port from args, default to 3000
    (init!)
    (start-server! port)))