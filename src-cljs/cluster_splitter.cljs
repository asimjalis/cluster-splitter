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

;[Todo]
; Add tests for the ip->machine->cluster->str process
; Rename machine-seq->4-or-5-cluster-seq 
; Publish on real endpoint
; Use - in name instead of _
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

(defn ip-seq->machine-seq [ip-seq]
  (->> ip-seq (partition 2) (map vec)))

(defn machine-seq->cluster-seq [machines-per-cluster machine-seq]
  (->> machine-seq (partition machines-per-cluster) (map vec)))

(defn merge-cluster-seqs [seq1 seq2]
  (map #(into %1 %2) seq1 seq2))

(defn machine-seq->4-cluster-seq [machine-seq]
  (->> machine-seq (machine-seq->cluster-seq 4)))

(defn-let machine-seq->5-cluster-seq [machine-seq]
  machine-count (count machine-seq)
  large-machine-count (/ machine-count 5)
  large-cluster-seq (->> machine-seq (take large-machine-count) (machine-seq->cluster-seq 1))
  small-cluster-seq (->> machine-seq (drop large-machine-count) (machine-seq->cluster-seq 4))
  cluster-seq (merge-cluster-seqs large-cluster-seq small-cluster-seq))

(defn machine-seq->4-or-5-cluster-seq [machines-per-cluster machine-seq]
  (if (= 5 machines-per-cluster) 
     (machine-seq->5-cluster-seq machine-seq)
     (machine-seq->4-cluster-seq machine-seq)))

(defn-test machine-seq->4-or-5-cluster-seq
  [ [ :a :b :c :d ]                       ] [ 4 [ :a :b :c :d ] ]
  [ [ :a :b :c :d ]       [ :e :f :g :h ] ] [ 4 [ :a :b :c :d :e :f :g :h ] ]
  [ [ :x :a :b :c :d ]                    ] [ 5 [ :x    :a :b :c :d ] ]
  [ [ :x :a :b :c :d ] [ :y :e :f :g :h ] ] [ 5 [ :x :y :a :b :c :d :e :f :g :h ] ]
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
