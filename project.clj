(defproject cluster_splitter "1.1.0-SNAPSHOT"
  :description "Splits IP addresses into small training clusters."
  :dependencies [
    [org.clojure/clojurescript "0.0-2371"]
    [org.clojure/clojure "1.6.0"]
    [asimjalis/useful "1.1.0-SNAPSHOT"]
    ]
  :plugins [
    [lein-cljsbuild "1.0.3"]
    [com.cemerick/clojurescript.test "0.3.1"] ]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
    :builds {
      :dev
      {:source-paths ["src-cljs"]
       :compiler {:output-to "cluster_splitter.js"
                  :optimizations :whitespace
                  :pretty-print true}}
      :prod
      {:source-paths ["src-cljs"]
       :compiler {:output-to "cluster_splitter.js"
                  :optimizations :advanced
                  :externs  [ "js/jquery-1.9.1-externs.js" ]
                  :pretty-print true}}}})
