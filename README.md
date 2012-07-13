Plastic
=======

Plastic provides a Clojure library that makes it easier to work with
the [Elasticsearch](http://www.elasticsearch.org/) Java API. It
provides a small and fairly concise set of functions that cover most
of what you might need to get data in and out of Elasticsearch.

Wherever we can, we model the data structures we use on the REST
API. For instance, when creating a new index or document, you'll find
that the map you create in Clojure is very similar (in fact,
syntactically identical) to the JSON object. Except, of course, that
you can stuff a byte array into it.

We're currently using this library in several projects and are
committed to improving the library and adding additional
functionality. Stay tuned for more updates.

[Function documentation](http://cmiles74.github.com/plastic/) is also
available for this project. This library is licensed under both the
[GPL](https://github.com/tnr-global/bishop/blob/master/GPL-LICENSE.txt)
and
[MIT](https://github.com/tnr-global/bishop/blob/master/MIT-LICENSE.txt)
licenses.

Installation
------------

To use Plastic, add the following to your project’s “:dependencies”:

    [tnrglobal/plastic "0.5.2"]

As of this writing, Plastic works (and has been tested against)
[Elasticsearch 0.19.4](http://www.elasticsearch.org/blog/2012/05/21/0.19.4-released.html). This
is the version required by the project, you might get things working
with a newer or older version of the Elasticsearch but it's not
something that I've tried.

Using the Library
-----------------

This library is a work in progress and not all of the Elasticsearch
functionality is exposed through this library. I think there's enough
here to make it useful but let me know if there's something missing
that you simply cannot live without.

Plastic is designed to work with standard Clojure data types,
documents are expressed as maps. Everywhere it can, Plastic uses SMILE
when communicating with Elasticsearch; if you need to store a raw
byte-array you shouldn't have any problems.

### Creating a New Index

When creating a new index, the first step is to specify your index
settings and mappings.

    (def customers-settings
      {:number_of_shards 5
	   :number_of_replicas 5
	   :analysis {:analyzer {:email_analyzer {:type "custom"
                                              :tokenizer "uax_url_email"}}}})

	(def customers-mappings
	   [{"customer" {:customer {:properties
	                             {:email {:type "string"
	                                      :analyzer "email_analyzer"}
							      :first_name {:type "string"}
								  :last_name {:type "string"}}}}}])

Next you'll need to connect to your Elasticsearch cluster with the
TransportClient.

    (plastic/client "elasticsearch"
  	            	9300
                    ["33.33.33.10", "33.33.33.11", "33.33.33.12"])

You provide the name of your clsuter, the port that your nodes are
communication on and a sequence with the names or IP addresses of your
cluster nodes. Plastic provides a "with-client" macro that you can use
to close your client after use.

Here we define an index with five shards, five replicas, one custom
analyzer for e-mail addresses and one mapping for the type
"customer". With our index defined, we can go ahead and create the
index on our cluster.

    (plastic/with-client
	  (plastic/client "elasticsearch"
  					  9300
                      ["33.33.33.10", "33.33.33.11", "33.33.33.12"]
	  (plastic/create "customers" customers-settings customers-mappings)

If everything works as expected, you're receive a boolean "true" as
the return value. If not, an exception will likely be thrown.

The "with-client" macro will open up a new connection to our cluster
and close it when we're done. The "create" function will create our
new index, "customers", and create our mappings.

### Indexing Documents

The basic idea is that you represent your documents as maps and then
index them with Elasticsearch. Later on, you can either fetch them by
unique ID or perform searches against indexed fields to get them back
out. You can define your own unique ID when you index your document or
let Elasticsearch choose one for you.

    (plastic/with-client
	  (plastic/client "elasticsearch"
	                  9300
                      ["33.33.33.10", "33.33.33.11", "33.33.33.12"])
	  (plastic/index "customers"
	                 "customer"
	                 {:first_name "Christopher"
					  :last_name "Miles"
					  :email "twitch@nervestaple.com"}
				     "12039202"))

Here we tell Plastic to use the "customers" index and that the
document we're providing is of the type "customer" (it should use our
"customer" mappings). We then provide the map that represents our
document and the unique ID we'd like to assing. The stanza above will
return a map indicating that your document has been indexed. As you
can see, the unique ID we provided was used.

    {:id "12039202", :index "customers", :type "customer",
	 :version 1, :matches nil}

### Fetching Documents

Once you have some documents stored in Elasticsearch, you can perform
searches against the indexed fields or you can fetch a document by
it's unique ID. Fetching by ID (in Elasticsearch terms a
["Get"](http://www.elasticsearch.org/guide/reference/api/get.html))
hace the added benefit of being realtime. This means that as soon as
you index a document you can reliably fetch it by ID, regardless of
how long it takes to actually be indexed.

    (plastic/with-client
	  (plastic/client "elasticsearch"
	                  9300
                      ["33.33.33.10", "33.33.33.11", "33.33.33.12"])
	  (plastic/fetch "customers" "customer" "12039202")

We tell Plastic to use the "customers" index and to fetch only
documents of the type "customer", we then provide the unique ID of the
document we want to fetch. The map that represents the document is
returned.

    {:last_name "Miles", :email "twitch@nervestaple.com",
	 :first_name "Christopher"}

If you look at the metadata for that map, you can see the ID, the
version, the index that stored it as well as it's type.

    {:exists true, :fields nil, :id "12039202", :version 1,
	 :index "customers", :type "customer"}

### Searching for Documents

Searching is, of course, a breeze. You tell Plastic which index you'd
like to use, what type of documents you'd like retrieved and then
provide a query using the
[Elasticsearch Query DSL](http://www.elasticsearch.org/guide/reference/query-dsl/).

    (plastic/with-client "elasticsearch"
                         ["33.33.33.10", "33.33.33.11", "33.33.33.12"]
	 (plastic/search "customers" {:query {:match_all {}}}))

In this example we ask for all documents of type "customer" and
receive a map of the results.

    {:hits
	  {:hits ([{:last_name "Miles", :email "twitch@nervestaple.com",
	            :first_name "Christopher"}]),
	    :max-score 1.0, :total-hits 1}, :facets nil, :failed-shards 0,
		:shard-failures nil, :status #<RestStatus OK>, :successful-shards 1,
		:timed-out false, :took 3, :total-shards 1}

Elasticsearch returns a map of "hits", that is matching documents. The
sequence under the :hits key contains the matching maps in pages (10
by default). By passing in a value under the :from key, you can page
through these results. You can also tell Plastic to fetch the results
lazily, after you process the first page of results the next page will
be fetched in the background and it'll be available when you try to
use it.

Future Plans
------------

Those are just the basics, I'll add more documentation as I get to
it. For now, take a look at the
[unit tests](https://github.com/cmiles74/plastic/blob/master/test/com/tnrglobal/plastic/test/core.clj)
and check out our
[function documentation](http://cmiles74.github.com/plastic/).
