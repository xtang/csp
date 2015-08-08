(ns csp.mockup
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]
            [org.httpkit.client :as http]))

(defn report-error [response]
  (println "Error" (:status response) "retrieving URL:" (get-in response [:opts :url])))

(defn http-get-response [ch url]
  (do
    (Thread/sleep 20000)
    (http/get url (fn [response]
                    (if (= 200 (:status response))
                      (put! ch (:body response))
                      (do
                        (report-error response)
                        (close! ch)))))))

;;返回某一个页面的内容 超时则返回nil
(defn crawler-with-timeout
  [url]
  (let [ch (chan)
        t (timeout 10000)]
    (do (go
          (alt!
            ch ([x] (println "Read" x "from channel"))
            t (println "Timed out")))
        (http-get-response ch url))))

(crawler-with-timeout "https://www.baidu.com/")
