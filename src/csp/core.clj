(ns csp.core
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]
            [clojure.java.io :as io]
            [csp.csp-async :refer [http-get new-links]]))

(defn get-words
  [text]
  (re-seq #"\w+" text))

(defn get-counts
  [urls]
  (let [counts (chan)]
    (go (while true
          (let [url (<! urls)]
            (when-let [response (<! (http-get url))]
              (let [c (count (get-words (:body response)))]
                (>! counts [url c]))))))
    counts))

(defn get-counts-for-feeds
  [feeds-file]
  (with-open [rdr (io/reader feeds-file)]
    (let [feed-urls (line-seq rdr)
          article-urls (doall (map new-links feed-urls))
          article-counts (doall (map get-counts article-urls))
          counts (async/merge article-counts)]
      (while true
        (prn (<!! counts))))))

(defn -main
  [feeds-file]
  (get-counts-for-feeds feeds-file))
