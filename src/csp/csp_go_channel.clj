(ns csp.csp-go-channel
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take]]))

(comment
  ;;will cause -> CompilerException clojure.lang.ExceptionInfo: Can only recur from tail position
  (defn map-chan [f from]
    (let [to (chan)]
      (go-loop []
        (when-let [x (<! from)]
          (>! to (f x))
          (recur))
        (close! to))
      to)))

;;一个go block中只处理一个channel
(defn map-chan
  [func from]
  (let [to (chan)]
    (go-loop [x (<! from)]
      (if x
        (do
          (>! to (func x))
          (recur (<! from)))
        (close! to)))
    to))

(let [ch (chan 10)
      mapped (map-chan (partial * 2) ch)]
  (do
    (onto-chan ch (range 0 10))
    (<!! (async/into [] mapped))))

;;core.async内置了专门针对channel的基础操作
(let [ch (to-chan (range 0 10))]
  (<!! (async/into [] (map< (partial * 2) (filter< even? ch)))))

;;用go channel实现一个并发版本的素数筛
(defn factor?
  [x y]
  (zero? (mod y x)))

;;prime -> 大于1的自然数中,除了1和此整数自身外,无法被其他自然数整除的数
(defn get-primes
  [limit]
  (let [primes (chan)
        numbers (to-chan (range 2 limit))]
    (go-loop [ch numbers
              prime (<! ch)]
      (if prime
        (do
          (>! primes prime)
          (let [filter-numbers (remove< (partial factor? prime) ch)]
            (recur filter-numbers (<! filter-numbers))))
        (close! primes)))
    primes))

;;get-primes返回的还是一个channel
(defn prn-primes
  [limit]
  (let [primes (get-primes limit)]
    (loop []
      (when-let [prime (<!! primes)]
        (println prime)
        (recur)))))

(prn-primes 100)

;;一个go block中调度多个channel
(let [ch1 (chan)
      ch2 (chan)]
  (do (go-loop []
        (alt!
          ch1 ([x] (println "Read" x "from channel 1"))
          ch2 ([x] (println "Twice" x "is" (* x 2))))
        (recur))
      (>!! ch1 "foo")
      (>!! ch2 3)
      (>!! ch1 "haha")
      (>!! ch2 6)))

;;timeout
;;timeout会返回一个channel 这个channel在指定时间(以毫秒为单位)之后就会被关闭
(time (<!! (timeout 10000)))

;;timeout和alt!一起使用可以为channel操作设置超时时间
;;假如在指定时间没有往ch放入数据 那么就会直接打印Timed out
(let [ch (chan)
      t (timeout 10000)]
  (do (go
        (alt!
          ch ([x] (println "Read" x "from channel"))
          t (println "Timed out")))
      (Thread/sleep 5000)
      (>!! ch 3)))

;;使用timeout来实现在规定超时时间内获取尽可能多的素数的功能
(defn get-primes-infinit
  []
  (let [primes (chan)
        numbers (to-chan (iterate inc 2))]
    (go-loop [ch numbers
              prime (<! ch)]
      (if prime
        (do
          (>! primes prime)
          (let [filter-numbers (remove< (partial factor? prime) ch)]
            (recur filter-numbers (<! filter-numbers))))
        (close! primes)))
    primes))

(defn prn-primes-infinit
  []
  (let [primes (get-primes-infinit)
        limit (timeout 10000)]
    (loop []
      (alt!! :priority true
        limit nil
        primes ([prime] (println prime) (recur))))))

(prn-primes-infinit)