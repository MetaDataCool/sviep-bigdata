This is a Clojure programmatic/REPL interface in a UC Berkeley project around text analysis. It features : 
* A scraper of a website that has data on US Non-Profit organizations (in [sviepbd.crawler](src/sviepbd/crawler.clj))
* A scraper / indexer for the search query results of a famous search engine (in [sviepbd.google.crawler](src/sviepbd/google/crawler.clj))
* utilities for tokenizing (wrapping Stanford CoreNLP), scraping (wrapping JSoup), and fast web pages fetching (using the httpkit asynchronous HTTP client and clojure.core.async)
* A homemade implementation of the Sparse PCA algorithm, using a hacked version of the la4j matrix library.

## Prerequisites

The prerequisites for using this are :
* Having Java
* Having the MongoDB database engine (and having an local instance running when reading/writing to it, at `mongodb://localhost:27017`)
* Having Leiningen 2 for running the Clojure project
* Having a data.db file in the resources folder when SQLite gets involved (for indexing)
* Having the project next to a directory named `data` (for sparse matrix exports)
