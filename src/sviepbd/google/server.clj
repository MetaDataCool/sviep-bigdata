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
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            
            [clojure.core.async :as a]
            [org.httpkit.client :as http]
            
            [sviepbd.google.crawler :as crawler])
  (:use clojure.repl clojure.pprint))

;; ----------------------------------------------------------------
;; Requests handlers
;; ----------------------------------------------------------------

(def ^:private completed-scrapings-chan-in (a/chan 32))
(def completed-scrapings-mult (a/mult completed-scrapings-chan-in))

(defn send-completions-to-hooks! []
  (let [hooks (if-let [url (java.lang.System/getenv "ONCOMPLETE_HOOK")] [url] ["http://www.google.com"])
        c (a/chan 32)]
    (a/tap completed-scrapings-mult c)
    (a/go-loop
      [] (when-let [data (a/<! c)]
           (doseq [url hooks] (http/post url {:body (update-in data [:query_id] str)}))
           (recur)))
    (log/info (str "Ready to send completions data to " hooks))
    ))
(defn save-completions-to-db! []
  (let [coll-name "completions"
        c (a/chan 32)]
    (a/go-loop [] (when-let [data (a/<! c)] (-> (monger.collection/insert coll-name data) a/thread a/<!) (recur)))
    (a/tap completed-scrapings-mult c)
    (log/info (str "Ready to save completions data in collection " coll-name))))

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
    ;; requests go from bottom to top, responses from top to bottom
    
    (ring-json/wrap-json-body {:keywords? true})
    ring-json/wrap-json-response
    
    (wrap-defaults api-defaults)
    ))

(def server-state (atom {:stop-server! nil
                         :port nil}))

;; ----------------------------------------------------------------
;; Server
;; ----------------------------------------------------------------

(defn init! []
  (log/info "Connecting to MongoDB...")
  (crawler/connect-mongo!)
  ;(send-completions-to-hooks!)
  (save-completions-to-db!)
  )

(def server-state (atom {:stop-server! nil
                         :port nil}))
(defn start-server! [port]
  (reset! server-state {:stop-server! (server/run-server #(app %) {:port port})
                        :port port}))
(defn stop-server! []
  ((:stop-server! @server-state))
  (reset! server-state (atom {:stop-server! nil, :port nil})))

(defn -main
  "I don't do a whole lot ... yet."
  [& [port]]
  (let [port (-> port (or "3000") java.lang.Integer/parseInt) ;; get port from args, default to 3000
        ]
    (init!)
    (start-server! port)))