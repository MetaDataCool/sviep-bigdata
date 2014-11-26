For now there's only a webcrawler, the code is in [src/sviepbd/crawler.clj](src/sviepbd/crawler.clj).

It's in Clojure. I've used [http-kit](http://http-kit.org/client.html) as an HTTP client, [JSoup](http://jsoup.org/) for parsing HTML and DOM querying, and [cheshire](https://github.com/dakrone/cheshire) for JSON serialization. It all went very well.

Tips for running this:

You will need the Java Dev environment set up,
as well as a Clojure environment that you can download on Mac with homebrew: brew install leiningen

To launch the program you need to launch with a REPL project.cli

Then in the command

You also need mongodb installed up and running with defaults

Then, to launch the program you need to launch with a REPL project.cli

Then in the command  line type : (perform-scraping!)

Results will be put in the mongo db "sviep" in the collection "results"

To copy to SQLIte the results
do (connect-mongo!) in REPL


to create the db table in sqlite3:

(create-results-table!) 

to save the results to sqlite3:

(->>(get-results)
  (map save-result-to-sqlite!)
  dorun)


To copy the entire pre-tokenized mongo db of 

 - "results" : 9000 results, some in French with approximate ranking; the target websites have not been scraped. Tokenized.
- "results2": 548 results, only in English and with exact ranking (not very useful).
- "results_complete": 421 results documents (those of the above which websites have been fetched successfully). Tokenized.

that is in /results_exports into your mongodb
be sure to have your instance of mongod up and running and then do

mongorestore --db sviepbd path/to/results_exports/sviepbd

that will copy into your mongo instance the tokenized collections

tokenized means:"tokens_bag" key; it's an array of {"word": <word>, "weight": <number>}


Then the Python part:

main.py can be run and right now only import pymongo package to connect to the mongodb from python


