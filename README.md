For now there's only a webcrawler, the code is in [src/sviepbd/crawler.clj](src/sviepbd/crawler.clj).

It's in Clojure. I've used [http-kit](http://http-kit.org/client.html) as an HTTP client, [JSoup](http://jsoup.org/) for parsing HTML and DOM querying, and [cheshire](https://github.com/dakrone/cheshire) for JSON serialization. It all went very well.

Tips for running this:

You will need the Java Dev environment set up,
as well as a Clojure environment that you can download on Mac with homebrew: brew install leiningen

To launche the program you need to launch with a REPL project.cli

Then in the command

You also need mongodb installed up and running with defaults

Then, to launch the program you need to launch with a REPL project.cli

Then in the command  line type : (perform-scraping!)

Results will be put in the mongo db "sviep" in the collection "results"
