{:namespaces
 ({:source-url nil,
   :wiki-url "com.tnrglobal.plastic.core-api.html",
   :name "com.tnrglobal.plastic.core",
   :doc
   "Provides functions that make it easier to work with the\nElasticsearch clustered indexer and data store."}),
 :vars
 ({:arglists ([cluster-name port nodes]),
   :name "client",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/client",
   :doc "Creates a new Transport Client with the provided settings.",
   :var-type "function",
   :line 33,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([]),
   :name "cluster-client",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/cluster-client",
   :doc "Returns a ClusterAdminClient.",
   :var-type "function",
   :line 65,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([name] [name settings] [name settings mappings]),
   :name "create",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/create",
   :doc
   "Creates a new index with the provided name and map of settings and\nsequence of mappings (each mapping should have one key representing\nthe type and a map containing the mapping data. Returns false if the\ncreation failed.\n\nHere's an example:\n\n(create-index 'index' {:analysis\n                        {:analyser\n                          {:email_analyzer {:type 'custom'\n                                            :tokenizer 'uax_url_email'}}}}\n                      [{'customer' {:customer\n                                     {:properties\n                                       {:email {:type 'string'\n                                            :store yes\n                                            :analyzer 'email_analyzer'}}}])",
   :var-type "function",
   :line 188,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists
   ([index type id & {:keys [refresh], :or [refresh false]}]),
   :name "delete",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/delete",
   :doc
   "Deletes the document with the specified type and ID from the provided\nindex.\n\nThe behavior of this function may be customized with the following\nkeys.\n\n  :refresh Executes a refresh after deleting, false",
   :var-type "function",
   :line 482,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index-names]),
   :name "delete-index",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/delete-index",
   :doc
   "Removes the named indexes and returns 'true' if the deletion has\nbeen acknowledged by all cluster nodes.",
   :var-type "function",
   :line 294,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists
   ([index query]
    [index query types & {:keys [refresh], :or [refresh false]}]),
   :name "delete-query",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/delete-query",
   :doc
   "Runs the provided query on the specified index and deletes all of\nthe matching documents. If types are provided then only documents\nwith a matching type will be deleted.\n\nNote that because there isn't any faceting or sorting, this function\nexpects a query in the form {:term {:name \"Joe\"}}, queries in the\nform {:query {:term {:name \"Joe\"}}} will fail.",
   :var-type "function",
   :line 499,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index type id]),
   :name "fetch",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/fetch",
   :doc
   "Fetches the document from the provided index with the specified id.",
   :var-type "function",
   :line 252,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists
   ([index-name type data]
    [index-name
     type
     data
     id
     &
     {:keys [refresh], :or [refresh false]}]),
   :name "index",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/index",
   :doc
   "Indexes the provided map of data. The index-name and type\nparameters should be strings, the value in the id parameter will be\ncast to a String before indexing. If no id parameter is provided,\nElasticsearch will generate an id for the item.\n\nThe behvaior of this function may be customized with the following\nkeys.\n\n  :refresh Execute a refresh immediately after indexing, false",
   :var-type "function",
   :line 224,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index-names]),
   :name "index-settings",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/index-settings",
   :doc
   "Returns the current settings and mappings for the specified index..",
   :var-type "function",
   :line 529,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([]),
   :name "indices-client",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/indices-client",
   :doc "Returns an IndicesAdminClient.",
   :var-type "function",
   :line 60,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([indices-in]),
   :name "indices-exists?",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/indices-exists?",
   :doc "Returns true if all of the named indices exist.",
   :var-type "function",
   :line 170,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists
   ([index query-map]
    [index
     query-map
     types
     &
     {:keys [keep-alive size type],
      :or {keep-alive "120s", size 10, type :query-then-fetch}}]),
   :name "scroll",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/scroll",
   :doc
   "Runs the provided query on the specified index, if types are\nspecified then only documents with a matching type will be\nreturned. The query is represent by a map, it may contain the\nfollowing keys: 'query', 'facets', and 'filter'. This function will\nopen a 'scroll' on the Elasticsearch cluster, the sequence stored\nunder the :hits key will be lazy. By default the results are\nprovided in pages of ten, in the :hits map under the :hits key\ncontains a sequence and each document in the sequence will be ten\ndocuments long or less.\n\nNote that when you scroll over a result set, you need to keep the\nclient connection open (complete all processing within a\n'(with-client ...)' form.\n\nThis behavior of this function may be customized with the following\nkeys.\n\n  :keep-alive Length of time the scroll lives on the cluster, \"5s\"\n  :size The size of each page of results, 10\n  :type The type of search, :query-then-fetch",
   :var-type "function",
   :line 332,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists
   ([index-name query-map]
    [index-name
     query-map
     types
     &
     {:keys [from size lazy type],
      :or {from 0, size 10, lazy false, type :query-then-fetch}}]),
   :name "search",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/search",
   :doc
   "Runs the provided query on the specified index, if types are\nspecified then only documents with a matching type will be\nreturned. The query is represent by a map, it may contain the\nfollowing keys: 'query', 'facets', and 'filter'. Each key should\ncontain a map value. By default the results are provided in pages of\nten, in the :hits map under the :hits key contains a sequence and\neach document in the sequence will be ten documents long or\nless. You may provide a map of optional values, these include...\n\n  :size  The size of result pages, defaults to 10\n  :from  The page of results from which hits will be returned\n  :lazy  Setting this loads only one page lazily, false\n  :type  The type of search, a key into SEARCH-TYPES map\n\nThis function will handle paging automatically, returning a lazy\nsequence under the :hits key. By default this function will fetch\nresults ten at a time, set the :lazy key to false to load only the\nspecified :from page (0 is assumed if no :from page is\nprovided). Note that when you page over a result set lazily, you\nneed to keep the client connection open (complete all processing\nwithin a '(with-client ...)' form.",
   :var-type "function",
   :line 404,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index type id update-script]),
   :name "update",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/update",
   :doc
   "Updates the document in the specified index with the provided type and\nid with the update script map. This effectively performs a merge of\nthe document and the map and then re-indexes the full document. Note\nthat the update-script is *not* Clojure code, this should be an MVEL\nscript.\n\n  {:script 'ctx._source.counter += count'\n   :params {:count 1}}\n\nThe update-script map above will increment the counter field of the\ntarget document.\n\n  http://mvel.codehaus.org/",
   :var-type "function",
   :line 258,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index-names mapping]),
   :name "update-index-mapping",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/update-index-mapping",
   :doc
   "Updates the mapping for the specified indexes with the provided map\nof data. The provided map of data should include only one type\nmapping. Returns true if the update was successfully enacted. A\nsample mapping follows.\n\n{:thing1 {:properties {:name {:type \"string\"\n                              :index \"not_analyzed\"}}}}",
   :var-type "function",
   :line 568,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index-names settings-map]),
   :name "update-index-settings",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/update-index-settings",
   :doc
   "Updates the settings for the specified indexes with the provided\nmap of data. Returns true if the update was successfully enacted.",
   :var-type "function",
   :line 548,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([index-names]),
   :name "wait-for-green",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/wait-for-green",
   :doc
   "Returns once the provided index has reached the \"green\" health\nstatus. You may provide the name of an index or a sequence of index\nnames.",
   :var-type "function",
   :line 178,
   :file "src/com/tnrglobal/plastic/core.clj"}
  {:arglists ([client & body]),
   :name "with-client",
   :namespace "com.tnrglobal.plastic.core",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/com.tnrglobal.plastic.core-api.html#com.tnrglobal.plastic.core/with-client",
   :doc
   "Evaluates the provided forms in the context of the specified\nElasticsearch client. When the evaluation is complete, the\nElasticsearch client is closed.",
   :var-type "macro",
   :line 50,
   :file "src/com/tnrglobal/plastic/core.clj"})}
