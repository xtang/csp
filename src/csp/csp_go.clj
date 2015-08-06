(ns csp.csp-go
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]))

;;<!! >!!为阻塞版本 <! >!为等待版本 阻塞会阻塞当前的上下文 而等待只是切换了当前的上下文
;;<! >!只能在go block中使用 在go block中最好不要用阻塞版本的函数

;;thread新建的线程还是从线程池中获取的 势必会存在阻塞的问题
;;而如果用go块就可以将同步的代码转为异步的事件驱动的逻辑
(let [ch (chan)]
  (do (go
        (let [x (<! ch)
              y (<! ch)]
          (println "Sum:" (+ x y))))
      (>!! ch 3)
      (>!! ch 4)))

;;go block返回的还是一个channel
(<!! (go (+ 3 4)))

(defn go-add
  [x y]
  (<!! (nth (iterate #(go (inc (<! %))) (go x)) y)))

;;(iterate f x) just return a lazyseq (x (f x) (f (f x)) (f (f (f x))) ...)
(time (go-add 10 10))
