(ns csp.csp-channel
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]))

;;channel without buffer (sync)
;;在另一个线程中始终阻塞直到channel中被塞入数据

(let [c (chan)]
  (do
    (thread (println "Read:" (<!! c) "from c"))
    (>!! c "Hello thread")))

;;channel with buffer (async)
;;从已经关闭的空的channel中读出来的消息将会是nil 如果从未关闭的空的channel中读消息将会一直阻塞 直到有消息被放入channel中
(let [bc (chan 5)]
  (do
    (>!! bc 0)
    (>!! bc 1)
    (close! bc)
    (prn (<!! bc))
    (prn (<!! bc))
    (prn (<!! bc))))

(defn readall!!
  [channel]
  (loop [coll []]
    (if-let [x (<!! channel)]
      (recur (conj coll x))
      coll)))

(defn writeall!!
  [channel coll]
  (doseq [x coll]
    (>!! channel x))
  (close! channel))

(let [ch (chan 10)]
  (do
    (writeall!! ch (range 0 10))
    (readall!! ch)))

;;and we can use the library functions
;;async/into 返回的还是一个channel 只不过这个channel中是组合好的结果 所以还需要额外从channel读出结果的一步操作
(let [ch (chan 10)]
  (do
    (onto-chan ch (range 0 10))
    (<!! (async/into [] ch))))