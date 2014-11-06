(ns cluster_splitter
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

(defn-let foo [a]
  x a
  y (+ x 10)
  z (+ y 100))

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

(deftest test-something
  (is (= 3 3)))

(defn run-tests []
  (t/test-ns 'cluster_splitter))

(defn str->int 
  "Convert string to int or return default."
  [default x] 
  (let [x (js/parseInt x)] 
    (if (js/isNaN x) default x)))

(deftest ^:export test-str->int
  (is (= 10 (str->int 4 "10")))
  (is (= -1 (str->int 4 "-1")))
  (is (=  0 (str->int 4 "0")))
  (is (=  4 (str->int 4 "hello")))
  (is (=  4 (str->int 4 "")))
  (is (=  1 (str->int 4 "1.0")))
  (is (=  0 (str->int 4 "0.9")))
  (is (=  0 (str->int 4 "0.001"))))
  
(defn html->output [html] 
  (.html (js/$ "#output") html))

(defn str->ips
  "Extract IP addresses from text and return as vector." 
  [s]
  (->> s (re-seq #"\d+\.\d+\.\d+\.\d+")))

(defn clusters->str [clusters]
  (->> clusters 
    (map (fn [pairs] (->> pairs (map #(join "\t" %)) (join "\n")))) 
    (map-indexed #(str "Cluster " (inc %) "\n" %2)) (join "\n\n")))

(defn ips->4-cluster-str [ips]
  (->> ips 
    (partition 2) (map vec) 
    (partition 4) (map vec)
    clusters->str))

(defn merge-cluster-lists [cluster-list1 cluster-list2]
  (map #(into %1 %2) cluster-list1 cluster-list2))

(defn-let ips->5-cluster-str [ips]
  ip-count     (count ips)
  med-ip-count (/ ip-count 5)
  med-cluster-list (->> ips 
                     (take med-ip-count) 
                     (partition 2) (map vec)
                     (partition 1) (map vec))
  sml-cluster-list (->> ips 
                     (drop med-ip-count) 
                     (partition 2) (map vec)
                     (partition 4) (map vec))
  clusters (merge-cluster-lists med-cluster-list sml-cluster-list)
  cluster-str (->> clusters clusters->str))

(defn-let input-map->cluster-html [input-map]
  cluster-size (->> input-map :clusterSize (str->int 4))
  ips          (->> input-map :ipText str->ips)
  cluster-str  (if (= 5 cluster-size) 
                 (ips->5-cluster-str ips)
                 (ips->4-cluster-str ips))
  cluster-html (str "<pre>" cluster-str "</pre>"))

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
