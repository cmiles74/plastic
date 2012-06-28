(defproject tnrglobal/plastic "0.5"
  :description "A library to ease development with Elasticsearch"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.elasticsearch/elasticsearch "0.19.4"]
                 [cheshire "4.0.0"]]
  :dev-dependencies [[lein-autodoc "0.9.0"]
                     [swank-clojure/swank-clojure "1.3.3"]]
  :main com.tnrglobal.plastic.core)
