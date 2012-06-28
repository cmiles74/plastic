;;
;; Provides functions for managing the Elasticsearch client and other
;; related functions.
;;
(ns ^{:doc "Provides functions that make it easier to work with the
           Elasticsearch clustered indexer and data store."}
  com.tnrglobal.plastic.core
  (:use [cheshire.core])
  (:require [clojure.java.io :as io])
  (:import [org.elasticsearch.client.transport TransportClient]
           [org.elasticsearch.common.settings ImmutableSettings]
           [org.elasticsearch.common.transport InetSocketTransportAddress]
           [org.elasticsearch.action.admin.indices.exists IndicesExistsRequest]
           [org.elasticsearch.action.search SearchType]
           [org.elasticsearch.action.get GetRequest]
           [org.elasticsearch.action.index IndexRequest]
           [org.elasticsearch.action.update UpdateRequest]
           [org.elasticsearch.action.search SearchRequest]
           [org.elasticsearch.action.search SearchScrollRequest]))

;; Elasticsearch client
(def ^:dynamic *CLIENT* nil)

;; Seach types
(def SEARCH-TYPES
  {:dfs-query-and-fetch SearchType/DFS_QUERY_AND_FETCH
   :dfs-query-then-fetch SearchType/DFS_QUERY_THEN_FETCH
   :query-and-fetch SearchType/QUERY_AND_FETCH
   :query-then-fetch SearchType/QUERY_THEN_FETCH
   :count SearchType/COUNT
   :scan SearchType/SCAN})

(defn client
  "Creates a new Transport Client with the provided settings."
  [cluster-name port nodes]

  ;; setup our settings and client
  (let [settings (.build (.put (ImmutableSettings/settingsBuilder)
                               "cluster.name" cluster-name))
        tclient (TransportClient. settings)]

    ;; set our cluster nodes-
    (doall (for [node nodes]
             (.addTransportAddress tclient
                                   (InetSocketTransportAddress. node port))))
    tclient))

(def ^:dynamic *CLIENT* nil)

(defmacro with-client
  "Evaluates the provided forms in the context of the specified
  Elasticsearch client. When the evaluation is complete, the
  Elasticsearch client is closed."
  [client & body]
  `(binding [*CLIENT* ~client]
     (let [result# ~@body]
       (.close *CLIENT*)
       result#)))

(defn indices-client
  "Returns an IndicesAdminClient."
  []
  (.indices (.admin *CLIENT*)))

(defn cluster-client
  "Returns a ClusterAdminClient."
  []
  (.cluster (.admin *CLIENT*)))

(defn- index-response-map
  "Returns a map of the data contained in the provided index
  response."
  [index-response]
  {:id (.getId index-response)
   :index (.getIndex index-response)
   :type (.getType index-response)
   :version (.getVersion index-response)
   :matches (seq (.getMatches index-response))})

(defn- search-response-map
  "Returns a map of all of the search response data except for the
  hits."
  [response]
  {:facets (seq (.facets response))
   :failed-shards (.failedShards response)
   :shard-failures (seq (.shardFailures response))
   :status (.status response)
   :successful-shards (.successfulShards response)
   :timed-out (.timedOut response)
   :took (.tookInMillis response)
   :total-shards (.totalShards response)})

(defn- hits-map
  "Returns a map of all the hits response data except for the actual hits."
  [hits]
  {:max-score (.maxScore hits)
   :total-hits (.totalHits hits)})

(defn- hit-map
  "Returns a map of all the data in the provided hit."
  [hit]
  (let [hit-data (parse-smile (.source hit) true)]
    (with-meta (if hit-data hit-data {})
      {:explanation (.explanation hit)
       :fields (seq (.fields hit))
       :id (.getId hit)
       :index (.getIndex hit)
       :matched-filters (seq (.matchedFilters hit))
       :score (.score hit)
       :shard (.shard hit)
       :sort-values (seq (.sortValues hit))
       :type (.type hit)})))

(defn- get-map
  "Returns a map of all the data in the provided get result."
  [get]
  (let [get-data (parse-smile (.source get) true)]
    (with-meta (if get-data get-data {})
      {:exists (.exists get)
       :fields (seq (.fields get))
       :id (.id get)
       :version (.version get)
       :index (.index get)
       :type (.type get)})))

(defn- delete-map
  "Returns a map of all the data in the provided delete result."
  [delete]
  {:index (.index delete)
   :id (.id delete)
   :not-found (.notFound delete)
   :version (.version delete)
   :type (.type delete)})

(defn- delete-query-map
  "Returns a map of all the data in the provided delete query
  result."
  [delete]
  {:failed-shards (.failedShards delete)
   :index (.index delete)
   :successful-shards (.successfulShards delete)
   :total-shards (.totalShards delete)})

(defn- cluster-state-response
  "Returns a map of the current cluster state as reflected in the
  provided response. Sadly, much of this data is returned as a map of
  Strings when integers would be more appropriate. You have been
  warned."
  [response]
  {:cluster_name (.value (.clusterName response))
   :indices (into {}
                  (for [metadata (.getIndices (.getMetaData (.state response)))]
                    (let [name (.getKey metadata)
                          data (.getValue metadata)]

                      {name
                       {:mappings
                        (into {} (for [mapping (.getMappings data)]
                                   (parse-string
                                    (String. (.uncompressed
                                              (.source (.getValue mapping))))
                                    true)))

                        :settings
                        (into {}
                              (for [[key-in val-in] (.getAsMap
                                                     (.getSettings data))]
                                {(keyword key-in) val-in}))}})))})

(defn indices-exists?
  "Returns true if all of the named indices exist."
  [indices-in]
  (let [indices (if (coll? indices-in) (into-array indices-in)
                    (into-array [indices-in]))]
    (.exists (.get (.exists (indices-client)
                            (IndicesExistsRequest. (into-array indices)))))))

(defn wait-for-green
  "Returns once the provided index has reached the \"green\" health
  status. You may provide the name of an index or a sequence of index
  names."
  [index-names]
  (.get (.execute (.setWaitForGreenStatus
                   (.prepareHealth (cluster-client)
                                   (into-array (if (coll? index-names)
                                                 index-names [index-names])))))))

(defn create
  "Creates a new index with the provided name and map of settings and
  sequence of mappings (each mapping should have one key representing
  the type and a map containing the mapping data. Returns false if the
  creation failed.

  Here's an example:

  (create-index 'index' {:analysis
                          {:analyser
                            {:email_analyzer {:type 'custom'
                                              :tokenizer 'uax_url_email'}}}}
                        [{'customer' {:customer
                                       {:properties
                                         {:email {:type 'string'
                                              :store yes
                                              :analyzer 'email_analyzer'}}}])"
  ([name] (create name nil nil))
  ([name settings] (create name settings nil))
  ([name settings mappings]

     ;; setup our request, mappings needs to be a seq
     (let [request (.prepareCreate (indices-client) name)]

       ;; set our settings
       (if settings
         (.setSettings request (generate-string settings)))

       ;; add all our mappings
       (doall (for [mapping mappings]
                (.addMapping request
                             (first (keys mapping))
                             (generate-string (mapping (first (keys mapping)))))))

       (.getAcknowledged (.get (.execute request))))))

(defn index
  "Indexes the provided map of data. The index-name and type
  parameters should be strings, the value in the id parameter will be
  cast to a String before indexing. If no id parameter is provided,
  Elasticsearch will generate an id for the item.

  The behvaior of this function may be customized with the following
  keys.

    :refresh Execute a refresh immediately after indexing, false"
  ([index-name type data] (index index-name type data nil))
  ([index-name type data id & {:keys [refresh]
                               :or [refresh false]}]
     (index-response-map

      ;; build a new index request
      (let [request (.prepareIndex *CLIENT* index-name type)]

        ;; set the index data
        (.setSource request (generate-smile data))
        (.setRefresh request true)

        ;; set the id , if we have it
        (if id
          (.setId request (str id)))

        (.get (.execute request))))))

(defn fetch
  "Fetches the document from the provided index with the specified id."
  [index type id]
  (let [result (.get (.get *CLIENT* (GetRequest. index type (str id))))]
    (get-map result)))

(defn update
  "Updates the document in the specified index with the provided type and
  id with the update script map. This effectively performs a merge of
  the document and the map and then re-indexes the full document. Note
  that the update-script is *not* Clojure code, this should be an MVEL
  script.

    {:script 'ctx._source.counter += count'
     :params {:count 1}}

  The update-script map above will increment the counter field of the
  target document.

    http://mvel.codehaus.org/"
  [index type id update-script]
  (let [request (.prepareUpdate *CLIENT* index type id)]

    ;; set the script
    (.setScript request (:script update-script))

    ;; set the language
    (if (:lang update-script)
      (.setScriptLang request (:lang update-script))
      (.setScriptLang request "mvel"))
    ;; add our parameters
    (doall (for [param (:params update-script)]
             (.addScriptParam request (name (first param)) (second param))))

    ;; fetch and return the result
    (let [result (.get (.execute request))]
      {:id (.id result)
       :index (.index result)
       :matches (.matches result)
       :type (.type result)
       :version (.version result)})))

(defn delete-index
  "Removes the named indexes and returns 'true' if the deletion has
  been acknowledged by all cluster nodes."
  [index-names]
  (let [client (indices-client)
        indices (if (coll? index-names)
                  (into-array index-names)
                  (into-array [index-names]))
        builder (.prepareDelete client indices)]

    ;; get our response
    (let [response (.get (.execute builder))]
      (.acknowledged response))))

(defn- lazy-scroll
  "Returns a lazy sequnce of the hits for the provided scroll. Returns
  a sequence of sequences, each one countaining a page of hits."
  ([scroll-id] (lazy-scroll scroll-id 0))
  ([scroll-id num-results-in]

     ;; get the next batch of hits
     (let [response (.get (.execute
                           (.prepareSearchScroll *CLIENT* scroll-id)))
           hits (.hits response)
           total-hits (.totalHits hits)
           count-hits (count (.hits hits))
           num-results (+ num-results-in count-hits)]

       ;; do we have all of the hits available?
       (if (> total-hits num-results)

         ;; recurse to get more results
         (lazy-seq (cons (into [] (doall (for [hit hits] (hit-map hit))))
                         (lazy-scroll scroll-id num-results)))

         ;; we have all of our results, return them
         (doall (for [hit hits] (hit-map hit)))))))

(defn scroll
  "Runs the provided query on the specified index, if types are
  specified then only documents with a matching type will be
  returned. The query is represent by a map, it may contain the
  following keys: 'query', 'facets', and 'filter'. This function will
  open a 'scroll' on the Elasticsearch cluster, the sequence stored
  under the :hits key will be lazy. By default the results are
  provided in pages of ten, in the :hits map under the :hits key
  contains a sequence and each document in the sequence will be ten
  documents long or less.

  Note that when you scroll over a result set, you need to keep the
  client connection open (complete all processing within a
  '(with-client ...)' form.

  This behavior of this function may be customized with the following
  keys.

    :keep-alive Length of time the scroll lives on the cluster, \"5s\"
    :size The size of each page of results, 10
    :type The type of search, :query-then-fetch"
  ([index query-map] (scroll index query-map []))
  ([index query-map types & {:keys [keep-alive size type]
                             :or {keep-alive "120s"
                                  size 10
                                  type :query-then-fetch}}]
     (let [indices (if (coll? index) index [index])
           types-in (if (coll? types) types [types])
           builder (.prepareSearch *CLIENT* (into-array indices))]

       ;; set search type
       (.setScroll builder keep-alive)

       ;; set our query
       (.setQuery builder (generate-smile (:query query-map)))

       ;; set facets
       (if (:facets query-map)
         (.setFacets builder (generate-smile (:facets query-map))))

       ;; set filter
       (if (:filter query-map)
         (.setFacets builder (generate-smile (:filter query-map))))

       ;; set to scroll results
       (.setSize builder size)

       ;; set search type
       (.setSearchType builder (SEARCH-TYPES type))

       ;; handle types
       (if (seq types-in)
         (.setTypes builder (into-array types-in)))

       ;; return a lazy sequence on the scrolled data
       (let [result (.get (.execute builder))
             scroll-id (.scrollId result)
             hits (.hits (.hits result))]

         ;; merge our response data with the hit sequence
         (assoc (search-response-map result)
           :hits
           (merge (hits-map (.hits result))

                  ;; return a lazy sequence of hits (each item a
                  ;; sequence of hits)
                  {:hits
                   (lazy-seq (cons
                              (into [] (for [hit hits]
                                         (hit-map hit)))
                              [(lazy-scroll scroll-id (count hits))]))}))))))

(defn search
  "Runs the provided query on the specified index, if types are
  specified then only documents with a matching type will be
  returned. The query is represent by a map, it may contain the
  following keys: 'query', 'facets', and 'filter'. Each key should
  contain a map value. By default the results are provided in pages of
  ten, in the :hits map under the :hits key contains a sequence and
  each document in the sequence will be ten documents long or
  less. You may provide a map of optional values, these include...

    :size  The size of result pages, defaults to 10
    :from  The page of results from which hits will be returned
    :lazy  Setting this loads only one page lazily, false
    :type  The type of search, a key into SEARCH-TYPES map

  This function will handle paging automatically, returning a lazy
  sequence under the :hits key. By default this function will fetch
  results ten at a time, set the :lazy key to false to load only the
  specified :from page (0 is assumed if no :from page is
  provided). Note that when you page over a result set lazily, you
  need to keep the client connection open (complete all processing
  within a '(with-client ...)' form."
  ([index-name query-map] (search index-name query-map []))
  ([index-name query-map types & {:keys [from size lazy type]
                                  :or {from 0
                                       size 10
                                       lazy false
                                       type :query-then-fetch}}]

     (let [indices (if (coll? index-name) index-name [index-name])
           types-in (if (coll? types) types [types])
           builder (.prepareSearch *CLIENT* (into-array indices))]

       ;; set our query
       (.setQuery builder (generate-smile (:query query-map)))

       ;; set facets
       (if (:facets query-map)
         (.setFacets builder (generate-smile (:facets query-map))))

       ;; set filter
       (if (:filter query-map)
         (.setFacets builder (generate-smile (:filter query-map))))

       ;; handle types
       (if (seq types-in)
         (.setTypes builder (into-array types-in)))

       ;; set search type
       (.setSearchType builder (SEARCH-TYPES type))

       ;; set our size and page
       (.setSize builder size)
       (if from (.setFrom builder from))

       (let [result (.get (.execute builder))
             total-hits (.totalHits (.hits result))
             hits (.hits (.hits result))
             hits-collected (+ (count hits) (* from size))]

         (assoc (search-response-map result)
           :hits
           (merge (hits-map (.hits result))

                  ;; return a lazy sequence of our hits (each item a
                  ;; chunk of hits)
                  (if (not= :count type)
                    {:hits
                     (lazy-seq
                       (cons (into [] (doall (for [hit hits] (hit-map hit))))
                             (if (and lazy (> total-hits hits-collected))
                               (do
                                 (:hits (:hits (search index-name
                                                       query-map
                                                       types
                                                       :type type
                                                       :from (+ from size)
                                                       :size size)))))))})))))))
(defn delete
  "Deletes the document with the specified type and ID from the provided
  index.

  The behavior of this function may be customized with the following
  keys.

    :refresh Executes a refresh after deleting, false"
  [index type id & {:keys [refresh] :or [refresh false]}]
  (let [request (.prepareDelete *CLIENT* index type (str id))]

    ;; handle refreshes
    (if refresh
      (.setRefresh request true))

    (delete-map (.get (.execute request)))))

(defn delete-query
  "Runs the provided query on the specified index and deletes all of
  the matching documents. If types are provided then only documents
  with a matching type will be deleted.

  Note that because there isn't any faceting or sorting, this function
  expects a query in the form {:term {:name \"Joe\"}}, queries in the
  form {:query {:term {:name \"Joe\"}}} will fail."
  ([index query] (delete-query index query []))
  ([index query types & {:keys [refresh]
                         :or [refresh false]}]

     (let [indices (if (coll? index) index [index])
           types-in (if (coll? types) types [types])
           builder (.prepareDeleteByQuery *CLIENT* (into-array indices))]

       ;; set our query
       (.setQuery builder (generate-smile query))

       ;; handle types
       (.setTypes builder (into-array types-in))

       ;; get our response
       (let [response (.get (.execute builder))]
         (with-meta
           (into []
                 (map delete-query-map
                      (seq response)))
           {:indices (seq (.getIndices response))})))))

(defn index-settings
  "Returns the current settings and mappings for the specified index.."
  [index-names]
  (let [client (cluster-client)
        indices (if (coll? index-names)
                  (into-array index-names)
                  (into-array [index-names]))
        builder (.prepareState client)]

    ;; we don't want everything
    (doto builder
      (.setFilterRoutingTable true)
      (.setFilterNodes true)
      (.setFilterIndices indices))

    ;; get our response
    (let [response (.get (.execute builder))]
      (cluster-state-response response))))

(defn update-index-settings
  "Updates the settings for the specified indexes with the provided
  map of data. Returns true if the update was successfully enacted."
  [index-names settings-map]
  (let [client (indices-client)
        indices (if (coll? index-names)
                  (into-array index-names)
                  (into-array [index-names]))
        builder (.prepareUpdateSettings client indices)]

    ;; set our new settings
    (.setSettings builder (generate-string settings-map))

    ;; get our response
    (let [response (.get (.execute builder))]

      ;; we fetched the response without issue, see
      ;; http://bit.ly/M77Qio
      true)))

(defn update-index-mapping
  "Updates the mapping for the specified indexes with the provided map
  of data. The provided map of data should include only one type
  mapping. Returns true if the update was successfully enacted. A
  sample mapping follows.

  {:thing1 {:properties {:name {:type \"string\"
                                :index \"not_analyzed\"}}}}"
  [index-names mapping]
  (let [client (indices-client)
        indices (if (coll? index-names)
                  (into-array index-names)
                  (into-array [index-names]))
        builder (.preparePutMapping client indices)]

    ;; set our new settings
    (.setType builder (name (first (keys mapping))))
    (.setSource builder (generate-string mapping))

    ;; get our response
    (let [response (.get (.execute builder))]
      (.acknowledged response))))