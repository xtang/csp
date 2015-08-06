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
