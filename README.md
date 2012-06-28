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

Installation
------------

To use Plastic, add the following to your project’s “:dependencies”:

[tnrglobal/plastic "0.5"]

Creating a New Index
--------------------

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
	                                      :tokenizer "email_analyzer"}}
							     {:first_name {:type "string"}}
								 {:last_name {:type "string}}}}}])

Here we define an index with five shards, five replicas, one custom
analyzer for e-mail addresses and one mapping for the type
"customer". With our index defined, we can go ahead and create the
index on our cluster.

    (plastic/with-client "elasticsearch"
                         ["33.33.33.10", "33.33.33.11", "33.33.33.12"]
	  (plastic/create "customers" customers-settings customers-mappings)

The "with-client" macro will open up a new connection to our cluster
and close it when we're done. The "create" function will create our
new index.

Indexing Documents
------------------

Indexing documents is similarly painless...

    (plastic/with-client "elasticsearch"
                         ["33.33.33.10", "33.33.33.11", "33.33.33.12"]
	  (plastic/index "customers" "customer"
	                 {:first_name "Christopher"
					  :last_name "Miles"
					  :email "twitch@nervestaple.com"}))

Searching for Documents
-----------------------

Searching is, of course, a breeze.

    (plastic/with-client "elasticsearch"
                         ["33.33.33.10", "33.33.33.11", "33.33.33.12"]
	 (plastic/search "customers" {:query {:match_all {}}}))

Those are just the basics, I'll add more documentation as I get to
it. For now, take a look at the
[unit tests](https://github.com/cmiles74/plastic/blob/master/test/com/tnrglobal/plastic/test/core.clj).
