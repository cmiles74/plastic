(defproject tnrglobal/plastic "1.0"
  :description "A library to ease development with Elasticsearch"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.elasticsearch/elasticsearch "0.19.4"]
                 [cheshire "4.0.0"]]
  :dev-dependencies [[org.clojure/tools.logging "0.2.3"]
                     [commons-logging "1.1.1"]
                     [log4j "1.2.16" :exclusions [javax.mail/mail
                                                  javax.jms/jms
                                                  com.sun.jdmk/jmxtools
                                                  com.sun.jmx/jmxri]]
                     [lein-autodoc "0.9.0"]
                     [swank-clojure/swank-clojure "1.3.3"]]
  :main com.tnrglobal.plastic.core)
