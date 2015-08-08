(ns csp.csp-async
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]
            [org.httpkit.client :as http]
            [clojure.java.io :as io])
  (:import [com.sun.syndication.io XmlReader SyndFeedInput]))

;;异步轮询
;;这和在go中使用alt!不同 这里的timeout会阻塞一整个go中的上下文执行 而用alt!可以让go中的各个channel在不同的上下文下 而一旦超时了 就一起返回结束了
;;所以这里的timeout的作用是做到间隔一段时间执行一段行为 而在alt中的timeout则是用来设置一个超时阈值 超过这个时间 所有alt中其他的channel都将失效被释放
(defn poll-fn
  [interval action]
  (let [seconds (* interval 1000)]
    (go (while true
          (action)
          (<! (timeout seconds))))))

;;每隔10秒轮训一次
;;(poll-fn 10 #(println "Polling at:" (System/currentTimeMillis)))

(comment
  ;;java.lang.AssertionError: Assert failed: <! used not in (go ...) block
  (let [ch (to-chan (iterate inc 0))]
    (poll-fn 10 #(println "Read:" (<! ch)))))

;;但是可以利用macro将外部的<! >!等注入到go block中 反正只要runtime的时候<! >!是在go block中执行的就行了
(defmacro poll
  [interval & body]
  `(let [seconds# (* ~interval 1000)]
     (go (while true
           (do ~@body)
           (<! (timeout seconds#))))))

;;每隔10秒进行轮询操作并且可以从外部的channel中拿出值
(defn poll-and-read
  [interval]
  (let [ch (to-chan (iterate inc 0))]
    (poll interval
      (println "Polling at:" (System/currentTimeMillis))
      (println (<! ch)))))

;;(poll-and-read 10)

(macroexpand-1
  '(poll 10
     (println "Polling at:" (System/currentTimeMillis))
     (println (<! ch))))

;;异步IO
;;同步IO是一个线程负责一个IO 会存在阻塞浪费CPU时间, 异步IO使用IO复用 可以连接多个IO连接 当一个IO连接有数据可以读写时则会有回调通知
;;但是一般的异步IO存在callback hell 类似nodejs这样的 用go channel的cps风格的异步IO可以让代码看上去更加像是顺序同步的 但是实际执行是异步的 提高可读性
(defn handle-response
  [response]
  (let [url (get-in response [:opts :url])
        status (:status response)]
    (println "Fetched:" url "with status:" status)))

;;(http/get "http://baidu.com" handle-response)

(defn report-error [response]
  (println "Error" (:status response) "retrieving URL:" (get-in response [:opts :url])))

;;put!只是向channel中扔一个消息 不在乎仍消息的结果 所以put!操作既不会阻塞也不暂停(上下文切换)
(defn http-get [url]
  (let [ch (chan)]
    (http/get url (fn [response]
                    (if (= 200 (:status response))
                      (put! ch response)
                      (do
                        (report-error response)
                        (close! ch)))))
    ch))

;;(http-get "http://baidu.com")

(def poll-interval 60)

(defn get-links
  [feed]
  (map #(.getLink %) (.getEntries feed)))

(defn parse-feed
  [body]
  (let [reader (XmlReader. (io/input-stream (.getBytes body)))]
    (.build (SyndFeedInput.) reader)))

(defn poll-feed
  [url]
  (let [ch (chan)]
    (poll poll-interval
      (when-let [response (<! (http-get url))]
        (let [feed (parse-feed (:body response))]
          (onto-chan ch (get-links feed) false))))
    ch))

(fn []
  (let [feed (poll-feed "http://www.cbsnews.com/latest/rss/main")]
    (loop []
      (when-let [url (<!! feed)]
        (prn url)
        (recur)))))

;;对解析feed得到的链接去重
(defn new-links
  [url]
  (let [in (poll-feed url)
        out (chan)]
    (go-loop [links #{}]
      (let [link (<! in)]
        (if (contains? links link)
          (recur links)
          (do
            (>! out link)
            (recur (conj links link))))))
    out))