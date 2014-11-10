(ns cluster-splitter
  (:use
    [clojure.string :only [join]]
    [clojure.walk :only [keywordize-keys]]
    ) 
  (:require 
    [clojure.browser.repl :as repl]
    [cemerick.cljs.test :as t]
    )
  (:require-macros 
    [clojure.template :as template]
    [asimjalis.useful :refer (defn-let defn-test)]
    [cemerick.cljs.test
      :refer (is deftest with-test run-tests testing test-var)]
      ))

; [Todo]
; Set up deploy JS script: cluster-splitter.html -> index.html, js->js
; Add [json-html "0.2.3"] (use 'json-html.core) https://github.com/yogthos/json-html

(defn debug-with-repl []
  (repl/connect "http://localhost:9000/repl"))

(defn console-log [& args]
  (.log js/console (apply str args)))

(defn form->map [] 
  "Convert form inputs with names to name-value map." 
  (->> (js/$ "form") 
    (.serializeArray)
    (js->clj)
    (map #(hash-map (% "name") (% "value")))
    (keywordize-keys) 
    (apply merge)))

(defn ^:export reload []
  (.reload js/location))
  

(defn ^:export run-tests []
  (t/test-ns 'cluster-splitter))

(defn str->int 
  "Convert string to int or return default."
  [default x] 
  (let [x (js/parseInt x)] 
    (if (js/isNaN x) default x)))

(defn-test 
  str->int 
  10 [4 "10"] 
  -1 [4 "-1"] 
  0  [4 "0"]
  4  [4 "hello"]
  4  [4 ""]
  1  [4 "1.0"]
  1  [4 "1.9"]
  1  [4 "1.001"])
  
(defn html->output [html] 
  (.html (js/$ "#output") html))

(defn str->ip-seq
  "Extract IP addresses from text and return as vector." 
  [s]
  (->> s (re-seq #"\d+\.\d+\.\d+\.\d+")))

(defn-test str->ip-seq
  ["1.1.1.1"]           ["hi 1.1.1.1 random text 2.2.2"]
  ["1.1.1.1" "2.2.2.2"] ["hi 1.1.1.1 random text 2.2.2.2"])

(defn ip-seq->machine-seq [ip-seq]
  (->> ip-seq (partition 2) (map vec)))

(defn machine-seq->cluster-seq [machines-per-cluster machine-seq]
  (->> machine-seq (partition machines-per-cluster) (map vec)))

(defn merge-cluster-seqs 
  "Merge two cluster seqs. If either cluster seq is empty then returns the other unchanged."
  [seq1 seq2]
  (cond (empty? seq1) seq2
        (empty? seq2) seq1
        :else (map #(into %1 %2) seq1 seq2)))

(defn-test merge-cluster-seqs 
  [ [:a :d] [:b :e] [:c :f] ] [ [[:a] [:b] [:c]] [[:d] [:e] [:f]] ]
  [ [:a   ] [:b   ] [:c   ] ] [ [[:a] [:b] [:c]] [              ] ]
  [ [   :d] [   :e] [   :f] ] [ [              ] [[:d] [:e] [:f]] ]
  )

(def L1 "Lion     ")
(def S1 "Elephant ")
(def S2 "Tiger    ")
(def S3 "Horse    ")
(def S4 "Monkey   ")

(def LARGE_MACHINE_NAMES [L1])
(def SMALL_MACHINE_NAMES [S1 S2 S3 S4])

(defn-let machine-seq-prepend-names [names machine-seq]
  names-cycle (->> names cycle)
  prepend-item-to-seq #(vec (cons %1 %2))
  _ (map prepend-item-to-seq names-cycle machine-seq))

(defn-test machine-seq-prepend-names 
  [ [:x :1] [:x :2] [:x :3] ]   [ [:x] [[:1] [:2] [:3]] ])

(defn-let machine-seq->4-cluster-seq [machine-seq]
  cluster-seq (->> machine-seq 
                   (machine-seq-prepend-names SMALL_MACHINE_NAMES)
                   (machine-seq->cluster-seq 4)))

(defn-let machine-seq->5-cluster-seq [machine-seq]
  machine-count (count machine-seq)
  large-machine-count (/ machine-count 5)
  large-machine-seq (->> machine-seq 
                         (take large-machine-count) 
                         (machine-seq-prepend-names LARGE_MACHINE_NAMES))
  small-machine-seq (->> machine-seq 
                         (drop large-machine-count) 
                         (machine-seq-prepend-names SMALL_MACHINE_NAMES))
  large-cluster-seq (->> large-machine-seq (machine-seq->cluster-seq 1))
  small-cluster-seq (->> small-machine-seq (machine-seq->cluster-seq 4))
  cluster-seq (merge-cluster-seqs large-cluster-seq small-cluster-seq))

(defn-test machine-seq->5-cluster-seq 
  [[[L1 0 1] [S1 4 5]   [S2 6 7]   [S3 8 9]   [S4 10 11]]
   [[L1 2 3] [S1 12 13] [S2 14 15] [S3 16 17] [S4 18 19]]]
  [(->> 20 range ip-seq->machine-seq)])

(defn-test machine-seq->4-cluster-seq 
  [[[S1 0 1] [S2 2 3] [S3 4 5] [S4 6 7]]]
  [(->> 8 range ip-seq->machine-seq)])

(defn machine-seq->4-or-5-cluster-seq [machines-per-cluster machine-seq]
  (if (= 5 machines-per-cluster) 
     (machine-seq->5-cluster-seq machine-seq)
     (machine-seq->4-cluster-seq machine-seq)))

(defn-test machine-seq->4-or-5-cluster-seq
  [ [         [S1 :a] [S2 :b] [S3 :c] [S4 :d] ] ] [ 4 [      [:a] [:b] [:c] [:d] ] ]
  [ [ [L1 :x] [S1 :a] [S2 :b] [S3 :c] [S4 :d] ] ] [ 5 [ [:x] [:a] [:b] [:c] [:d] ] ]
  )

(defn cluster-seq->str [cluster-seq]
  (->> cluster-seq 
    (map (fn [pairs] (->> pairs (map #(join "\t" %)) (join "\n")))) 
    (map-indexed #(str "Cluster " (inc %) "\n" %2)) (join "\n\n")))

(defn-test cluster-seq->str 
  (str "Cluster 1\n" "a\tb") 
    [ [ [ ["a" "b"] ] ] ]
  (str "Cluster 1\n" "a\tb\n" "c\td") 
    [ [ [ ["a" "b"] ["c" "d"] ] ] ]
  (str "Cluster 1\n" "a\tb" "\n\n" "Cluster 2\n" "c\td") 
    [ [ [ ["a" "b"] ] [ ["c" "d"] ] ] ]
  )

(defn-let input-map->cluster-html [input-map]
  machines-per-cluster  (->> input-map :clusterSize (str->int 4))
  ip-seq                (->> input-map :ipText str->ip-seq)
  machine-seq           (->> ip-seq ip-seq->machine-seq)
  cluster-seq           (->> machine-seq 
                          (machine-seq->4-or-5-cluster-seq machines-per-cluster))
  cluster-seq-str       (->> cluster-seq cluster-seq->str)
  cluster-html          (str "<pre>" cluster-seq-str "</pre>"))

(defn ^:export main []
  (->> (form->map) 
    (input-map->cluster-html) 
    (html->output)))

(defn init []
  (if (->> (form->map) :debug)
    (do 
      (debug-with-repl)
      (console-log "Welcome from REPL!"))))

(init)
