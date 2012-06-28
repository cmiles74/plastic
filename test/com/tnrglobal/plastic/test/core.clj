;;
;; Test suite for the Plastic library.
;;
(ns com.tnrglobal.plastic.test.core
  (:use [clojure.test]
        [clojure.tools.logging])
  (:require [com.tnrglobal.plastic.core :as plastic])
  (:import [org.elasticsearch.node NodeBuilder]
           [org.apache.commons.logging LogFactory]
           [org.apache.commons.logging Log]))

;; logger instance
(def LOGGER (. LogFactory getLog "plastic.test.core"))

(def test-index-def
  [;; settings
   {:number_of_shards 1
    :number_of_replicas 0
    :gateway {:type "none"}
    :index {:store {:type "memory"}}
    :analysis {:analyzer
               {:email_analyzer {:type "custom"
                                 :tokenizer "uax_url_email"}}}}
   ;; mappings
   [{"customer" {:customer
                 {:properties
                  {:email {:type "string"
                           :analyzer "email_analyzer"}
                   :name {:type "string"}
                   :age {:type "integer"}}}}
     "address" {:address
                {:properties
                 {:customer_email {:type "string"
                                   :index "not_analyzed"}}
                 {:street {:type "string"}}
                 {:city {:type "string"}}
                 {:state {:type "string"}}
                 {:zip {:type "string"
                        :index "not_analyzed"}}}}}]])

(def test-customers
  [{:name "Christopher Miles"
    :email "twitch@nervestaple.com"
    :age 38}
   {:name "Joanna Miles"
    :email "jadler.miles@gmail.com"
    :age 36}
   {:name "Emily Miles"
    :email "emily@nervestaple.com"
    :id "emily@nervestaple.com"
    :age 1}])

(def test-addresses
  [{:customer_email "twitch@nervestaple.com"
    :street "12 Clapp Street #A"
    :city "Easthampton"
    :state "MA"
    :zip "01027"}
   {:customer_email "jadler.miles@gmail.com"
    :street "213 West End Ave."
    :city "Smallville"
    :state "NH"
    :zip "19239"}
   {:customer_email "emily@nervestaple.com"
    :street "52 North Main Street"
    :city "Mystic"
    :state "CT"
    :zip "01108"}])

(defmacro with-test-node
  "Evaluates the provided expressions in the context of the test
  node. This node is closed after evaluation."
  [& body]
  `(let [node# (.node (.local (NodeBuilder/nodeBuilder) true))
         client# (.client node#)]
     (plastic/with-client client#
       (let [result# ~@body]
         (.close node#)
         result#))))

(defmacro with-test-index
  "Evaluates the provided expressions in the context of the test node
  and index (called 'test'). This index is removed and the node closed
  after evaluation or if an exception is thrown."
  [& body]
  `(with-test-node
     (do
       (plastic/create "test" (first test-index-def) (second test-index-def))
       (try
         (let [result# ~@body]
           result#)
         (finally
           (plastic/delete-index "test"))))))

(defmacro with-test-data
  "Evaluates the provided expressions in the context of the test node,
  index (called 'test') and data. This index is deleted and the node
  closed after evaluation."
  [& body]
  `(with-test-index
     (do

       (plastic/wait-for-green "test")

       ;; insert our customers
       (dorun (for [datum# test-customers]
                (plastic/index "test"
                               "customer"
                               datum#
                               (:email datum#)
                               :refresh true)))

       ;; insert our addresses
       (dorun (for [datum# test-addresses]
                (plastic/index "test"
                               "address"
                               datum#
                               nil
                               :refresh true)))
       (let [result# ~@body]
         result#))))

(deftest clients

  (testing "IndicesAdminClient"
    (with-test-node
      (let [indices-client (plastic/indices-client)]
        (is (instance? org.elasticsearch.client.IndicesAdminClient
                       indices-client)))))

  (testing "ClusterAdminClient"
    (with-test-node
      (let [cluster-client (plastic/cluster-client)]
        (is (instance? org.elasticsearch.client.ClusterAdminClient
                       cluster-client))))))

(deftest indexes

  (testing "Create and Delete Index"
    (with-test-node
      (let [create (plastic/create "test"
                                   (first test-index-def)
                                   (second test-index-def))
            delete (plastic/delete-index "test")]
        (is (and (= true create)
                 (= true delete))))))

  (testing "Index Exists, False"
    (with-test-node
      (let [result (plastic/indices-exists? "test")]
        (is (= false result)))))

  (testing "Index Exists"
    (with-test-index
      (let [result (plastic/indices-exists? "test")]
        (is (= true result))))))

(deftest data

  (testing "Index Data"
    (with-test-index
      (let [result (plastic/index "test"
                                  "customer"
                                  (first test-customers)
                                  (:email (first test-customers)))]
        (is (and (= (:email (first test-customers)) (:id result))
                 (= "test" (:index result))
                 (= "customer" (:type result))
                 (= 1 (:version result)))))))

  (testing "Fetch Data"
    (with-test-data
      (let [result (plastic/fetch "test" "customer" "emily@nervestaple.com")]
        (is (and (= "emily@nervestaple.com" (:email result))
                 (= "Emily Miles" (:name result)))))))

  (testing "Update Data"
    (with-test-data
      (let [original (plastic/fetch "test" "customer" "emily@nervestaple.com")
            update (plastic/update "test" "customer" "emily@nervestaple.com"
                                   {:script "ctx._source.name = name"
                                    :params {:name "Emily Adler Miles"}})
            updated (plastic/fetch "test" "customer" "emily@nervestaple.com")]
        (is (and (= "emily@nervestaple.com" (:email updated))
                 (= "Emily Adler Miles" (:name updated)))))))

  (testing "Search Data"
    (with-test-data
      (let [results (plastic/search "test" {:query {:match_all {}}})]
        (info (plastic/fetch "test" "customer" "emily@nervestaple.com"))
        (info "RESULTS: " results)
        (is (= 6 (:total-hits (:hits results)))))))

  (testing "Delete Data"
    (with-test-data
      (let [results-1 (plastic/search "test" {:query {:match_all {}}} "customer")]

        (plastic/delete "test"
                        "customer"
                        (:email (first test-customers))
                        :refresh true)

        (let [results-2 (plastic/search "test" {:query {:match_all {}}} "customer")]

          (is (and (= 3 (:total-hits (:hits results-1)))
                   (= 2 (:total-hits (:hits results-2)))))))))

  (testing "Delete by Query"
    (with-test-data
      (let [results-1 (plastic/search "test" {:query {:match_all {}}} "customer")]

        (plastic/delete-query "test"
                              {:field {:name "Christopher"}}
                              "customer")

        (let [results-2 (plastic/search "test" {:query {:match_all {}}} "customer")]

          (is (and (= 3 (:total-hits (:hits results-1)))
                   (= 2 (:total-hits (:hits results-2)))))))))

  (testing "Scroll"
    (with-test-data
      (let [result (plastic/scroll "test"
                                   {:query {:match_all {}}}
                                   "customer"
                                   :size 2)]

        (is (and (not (realized? (:hits (:hits result))))
                 (= 2 (count (first (:hits (:hits result)))))
                 (= 1 (count (second (:hits (:hits result))))))))))

  (testing "Search, Lazy"
    (with-test-data
      (let [result (plastic/search "test"
                                   {:query {:match_all {}}}
                                   "customer"
                                   :size 2
                                   :lazy true)]

        (is (and (not (realized? (:hits (:hits result))))
                 (= 2 (count (first (:hits (:hits result)))))
                 (= 1 (count (second (:hits (:hits result)))))))))))

(deftest metadata

  (testing "Fetch Index Settings"
    (with-test-data
      (let [response (plastic/index-settings "test")]

        (is (and (= "elasticsearch" (:cluster_name response))
                 (= "0" (:index.number_of_replicas (:settings ((:indices response)
                                                             "test"))))
                 (= "1" (:index.number_of_shards (:settings ((:indices response)
                                                           "test")))))))))

  (testing "Update Index Settings"
    (with-test-data
      (let [result (plastic/update-index-settings
                    "test" {:index.number_of_replicas 2})
            response (plastic/index-settings "test")]

        (is (and result
                 (= "elasticsearch" (:cluster_name response))
                 (= "2" (:index.number_of_replicas (:settings ((:indices response)
                                                             "test"))))
                 (= "1" (:index.number_of_shards (:settings ((:indices response)
                                                             "test")))))))))

  (testing "Update Index Mappings"
    (with-test-data
      (let [result (plastic/update-index-mapping
                    "test"
                    {:thing1 {:properties {:name {:type "string"
                                                  :index "not_analyzed"}}}})
            response (plastic/index-settings "test")]

        (is (and result
                 (= (:thing1 (:mappings ((:indices response) "test")))
                    {:properties {:name {:type "string"
                                         :index "not_analyzed"}}})))))))