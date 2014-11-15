For now there's only a webcrawler, the code is in [src/sviepbd/crawler.clj](src/sviepbd/crawler.clj).

It's in Clojure. I've used [http-kit](http://http-kit.org/client.html) as an HTTP client, [JSoup](http://jsoup.org/) for parsing HTML and DOM querying, and [cheshire](https://github.com/dakrone/cheshire) for JSON serialization. It all went very well.
