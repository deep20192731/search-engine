List of files in submission and a short description on what they do
1) Crawler.py - Class with actual Crawler flow implementation. Also main method in this files
2) WebCrawlerUtility.py - Utility module having functions used throughout application ex: readConfigFile
3) SeedPagesGetter.py - Module that gets seed-pages given query. Used Bing to get seed pages
4) OutputFlusher.py - Module dumping all the required crawled pages log to output file in required format
5) QueueWithDictionary.py - Module implementing SimpleQueueWithDictionary(for BFS) and PriorityQueueWithDictionary(for focused crawling)
6) Relevance.py - Module having functions that calculates relevance given texts/links/anchor-texts and query
7) DocumentParser.py - Module with all the functions that opens/extracts/validates links and texts from URL
8) CrawlerOutput.py - Simple POJO describing the output of the crawler which gets dumped to output-files
9) WebCrawlerLogger.py - Common Logger used throughout application. Flushes WARN to files and ERROR to files and screen
10) readme.txt - Describing succintly all the files and instructions on how to run the application
11) explain.txt - Detailed explanation of program-flow and what major functions are
12) crawler_conf.ini - Configuration file
13) /output/logs_focused_Q1_{datetime} - Output logs for Focused Crawling Strategy and Query1
14) /output/logs_focused_Q2_{datetime} - Output logs for Focused Crawling Strategy and Query2
15) /output/logs_bfs_Q1_{datetime} - Output logs for BFS Crawling Strategy and Query1
16) /output/logs_bfs_Q2_{datetime} - Output logs for BFS Crawling Strategy and Query2
17) crawler.log - LOG file with all the WARN's and ERROR's.

Description on how to run the program
Follow below instructions:
1) Make sure you have the following modules installed. If not, then use "pip install {module}"
	a) datetime
	b) urllib2
	c) posixpath
	d) html2text
	e) urlnorm
	f) urlparse
	g) BeautifulSoup
	h) reppy
	i) heapq
	j) math
	k) py_bing_search
	l) logging
	m) ConfigParser
2) Set strategy(bfs or focused) in crawler_conf.ini
2) run - "python Crawler.py"

Limitations on Parameters
When running the program, you will be asked to input query. Make sure to have atleast 2 words in the query, since for just one word
CosineSimilarity will not work well and the results wont be good