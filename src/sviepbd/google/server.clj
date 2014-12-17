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
            
            [sviepbd.google.crawler :as crawler])
  (:use clojure.repl clojure.pprint))

;; ----------------------------------------------------------------
;; Requests handlers
;; ----------------------------------------------------------------

(defn start-scraping [{:keys [query] :as body}]
  (-> {:message "coucou", :query query} r/response))

;; ----------------------------------------------------------------
;; Routes declaration
;; ----------------------------------------------------------------

(defroutes app-routes
  (POST "/start-scraping" {:keys [body]} (start-scraping body))
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
    (start-server! port)))