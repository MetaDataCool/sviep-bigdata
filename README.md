This is a Clojure programmatic/REPL interface in a UC Berkeley project around text analysis. It features : 
* A scraper of a website that has data on US Non-Profit organizations (in [sviepbd.crawler](src/sviepbd/crawler.clj))
* A scraper / indexer for the search query results of a famous search engine (in [sviepbd.google.crawler](src/sviepbd/google/crawler.clj))
* utilities for tokenizing (wrapping Stanford CoreNLP), scraping (wrapping JSoup), and fast web pages fetching (using the httpkit asynchronous HTTP client and clojure.core.async)
* A homemade implementation of the Sparse PCA algorithm, using a hacked version of the la4j matrix library.

# Crawler web app

This code can be used to deploy a web server that will expose HTTP web service endpoints which, when called, will start scraping for a query.

### API endpoints

Make a JSON `POST` request on endpoint `/start-scraping` with data of the form:
```json
{"query": "this is a google query"}
```
to start scraping. This will create a MongoDB document for the query, trigger the scraping and immediately return a document of the form :
```json
{
    "query_id": "54977c76e4b0a0eb7c20f317",
    "query": "this is a google query"
}
```

### MongoDB Collections

The database features the following collections:
- `queries`: When a the scraping is performed for a query, a document is saved here, holding an ID.
- `scraped_results`: Where the results of scraping (no indexation) are saved.
- `errors`: When errors occur during the scraping, the data about them will be saved here.
- `matrix_entries`: After indexation of the results of the query, documents of the form 
```javascript
{
    "_id" : ObjectId("549776ecd4c6b5a3eab49f6f"), // don't mind this
    "query_id" : ObjectId("54977686d4c6b5a3eab49d7b"), // the corresponding query
    "w" : 1, // the index of the word
    "v" : 1, // the numerical value of the matrix entry
    "r" : 394 // the index of the result
}
```
will be saved there.
- `words_tables`: After indexation of the results of the query, documents of the form 
```javascript
{
    "_id" : ObjectId("549776f0d4c6b5a3eab6afd8"), // never mind this
    "query_id" : ObjectId("54977686d4c6b5a3eab49d7b"), // the corresponding query
    "w" : "mining", // the word
    "k" : 1 // the word index 
}
```
will be saved there.
- `completions`: after full processing of a query (scraping + indexation), a document with useful information of the form
```javascript
{
    "_id" : ObjectId("54977749d4c6b5a3eab718b5"),
    "query_id" : ObjectId("54977686d4c6b5a3eab49d7b"),
    "query" : "mining",
    "errors_count" : 0,
    "results_count" : 499,
    "words_count" : 26846,
    "matrix_entries_count" : 135272
}
```
will be saved there.

### Deployment configuration

Configuration is done by setting the following environment variables:
- `MONGOLAB_URI` : a MongoDB URI for the database (not necessarily hosted on MongoLab though). Will default to `"mongodb://localhost:27017/sviepbd"` is not set.
- `GOOGLE_CALLS_INTERVAL_MS`: A nonegative integer, the minimum time interval between 2 HTTP calls to Google, in milliseconds. Used to globally limit the number of HTTP calls to Google. Will default to `0` if not set.

# Prerequisites

The prerequisites for using this are :
* Having Java
* Having the MongoDB database engine (and having an local instance running when reading/writing to it, at `mongodb://localhost:27017`)
* Having Leiningen 2 for running the Clojure project
* Having a data.db file in the resources folder when SQLite gets involved (for indexing)
* Having the project next to a directory named `data` (for sparse matrix exports)

# Using the crawler

The code for using the crawler is in namesepace [sviepbd.google.crawler](src/sviepbd/google/crawler.clj).

## Getting the sequence of crawled results

The crawler is organized as a pipeline of functions that process maps representing queries/pages/query results. Each function associates new information from a document returned by the previous function. You then process the crawling as a lazy sequence that goes through these functions using `map`, `mapcat` etc :

```clj
;; this gives you a lazy sequence of the crawled results : you typically store it into MongoDB.
(->> (result-pages "a search query") ;; a seq of result pages maps (a result page contains up to 10 query results)
  
  (pmap fetch-result-page) ;; fetching the HTML of each result page
  ;(map-pipeline-async fetch-result-page-a) ;; asynchronous equivalent of the above (faster)
  
  (mapcat get-search-results) ;; parses the result page HTML and splits its DOM into search results
  (map crawl-search-result) ;; crawls a search result element to extract the relevant information (text, link, etc.)
  
  (pmap fetch-website) ;; fetches the HTML of the website of the result. Note that it does not always succeed because of the format of the result.
  ;(map-pipeline-async fetch-website-a) ;; asynchronous equivalent of the above (faster)
  
  (pmap tokenize-result) ;; tokenizes the text fields of the data.
  )
```

This organizations enables you to easily test manually each step of the processing, to do it partly, store the results (MongoDB) and continue the processing later, and to decide how to structure your program for performance (parallel/pipelined/async ...).

Also, because these are _lazy_ Clojure sequences, you don't have to worry about fitting in memory: they will essentially be processed like streams of data.

An example of what a result looks like in JSON representation may be found [here](https://github.com/MetaDataCool/data/blob/eeff348b63883b66dcc3019be64492b6f18d0791/example-result.json).

## Using MongoDB for storing and retrieving crawling results

Before using MongoDB, make sure you have a MongoDB local server running at `localhost:27017`:
```
mongod --dbpath path/to/my/mongo/data/directory
```

and don't forget to connect the MongoDB client :
`(connect-mongo!)`

To store a lazy seq of results (see example above) in MongoDB, do something like :
```
(->> my-seq-of-results (map #(mc/save "results_collection_name" %)) dorun)
```

And to retrieve the results for the query `"my query"` : 
```
(mc/find-maps "results_collection_name" {:query "my query"})
```
