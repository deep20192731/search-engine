import sys
import time
import Relevance
from WebCrawlerUtility import WebCrawlerUtility
from SeedPagesGetter import SeedPagesGetter
from DocumentParser import DocumentParser
from OutputFlusher import OutputFlusher
from CrawlerOutput import CrawlerOutput
from WebCrawlerLogger import crawlerLogger
from QueueWithDictionary import SimpleQueueWithDictionary, PriorityQueueWithDictionary

'''
Main class responsible for facilitating the crawler flow
See 'run()' method for more details
'''
class Crawler:
    def __init__(self, outputFileName, seedPages, topic,
                 isBFSStrategy, thresholdForRelevance,
                 thresholdForLinksToParseOnOneSite, totalPagesToCrawl):
        self.seedPages = seedPages
        self.topic = topic
        self.isBFSStrategy = isBFSStrategy
        self.totalPagesToCrawl = totalPagesToCrawl
        self.thresholdForRelevance = thresholdForRelevance
        self.thresholdForLinksToParseOnOneSite = thresholdForLinksToParseOnOneSite

        self.flusher = OutputFlusher(outputFileName, totalPagesToCrawl)
        if(self.isBFSStrategy):
            self.que = SimpleQueueWithDictionary()
        else:
            self.que = PriorityQueueWithDictionary()

    def addToQueuePerFocusedStrategy(self, links, parentPageRelevance):
        '''
            Method used for Focused Crawling STRATEGY
            1) For each link, compute relevance score on hyperlink + query
            2) Combine the above two scores to get hyperlink promise
        '''
        linksWithGoodPromise = []
        for link, anchor in links:
            linkRelevanceScore = Relevance.getRelevanceScoreFromLinkAndQuery(anchor, self.topic)
            linkPromise = Relevance.combineScoreFromTextAndLink(parentPageRelevance, linkRelevanceScore)

            if(linkPromise > self.thresholdForRelevance):
                linksWithGoodPromise.append((linkPromise, link))

        linksWithGoodPromise.sort()
        for goodLink in linksWithGoodPromise[:self.thresholdForLinksToParseOnOneSite+1]:
            self.que.enqueue(goodLink[1], goodLink[0])

    def addToQueuePerBFSStrategy(self, links):
        links = [m[0] for m in links]
        self.que.enqueueItems(links[:self.thresholdForLinksToParseOnOneSite+1])

    def run(self):
        '''
            1) Put all seed-pages in Queue with priority=1. 0 <= Priority <= 1
            2) Start Dequeuing URLs and open them
            3) Parse contents for hyperlinks and text (according to rules)
            4) Compute relevance score based on text + query
            5) Put hyperlinks in PQ acc to startegy and dequeued URL to Output Buffer
        '''
        self.que.enqueueItems(self.seedPages)

        start = time.time()
        # As we explore we maintain Harvest Rate
        relevantLinksExploredTillNow = 0.0
        totalLinksExploredTillNow = 0.0
        harvestRate = 0.0

        # Some for overall stats
        totalLinks = 0
        totalSize = 0
        totalTime = 0
        totalErrorLinks = 0

        while(totalLinksExploredTillNow <= self.totalPagesToCrawl and not self.que.empty()):
            url = self.que.dequeue()

            text, pageSize, responseCode, linksToExplore = DocumentParser.extractLinksAndTextFromURL(url)
            totalLinks += 1
            totalSize += (pageSize if pageSize is not None else 0)

            if(responseCode == 200):
                if(text is None):
                    crawlerLogger.debug("Ignoring " + url + " since there was error in parsing")
                    continue

                totalLinksExploredTillNow += 1.0
                pageRelevanceScore = Relevance.getRelevanceScoreFromTextAndQuery(text, self.topic)

                try:
                    print url + " " + str(pageRelevanceScore)
                except:
                    print "Codec Error"

                # See if the page we are exploring is relevant or not
                if(pageRelevanceScore >= self.thresholdForRelevance):
                    relevantLinksExploredTillNow += 1.0

                if(self.isBFSStrategy):
                    self.addToQueuePerBFSStrategy(linksToExplore)
                else:
                    self.addToQueuePerFocusedStrategy(linksToExplore, pageRelevanceScore)

                # Write the crawled URL to output-logs
                harvestRate = float(relevantLinksExploredTillNow)/float(totalLinksExploredTillNow)
            else:
                totalErrorLinks += 1
            obj = CrawlerOutput(url, pageSize if pageSize is not None else "Error", responseCode if responseCode is not None else "ErrorCode", harvestRate)
            self.flusher.addToOutputBuffer(obj)

        totalTime = (time.time() - start) / 60
        self.flusher.flushOutput(str(totalLinks), str(totalSize), str(totalTime), str(totalErrorLinks))


if __name__ == '__main__':
    reload(sys)
    sys.setdefaultencoding("utf-8")

    configFileName = "crawler_conf.ini"
    crawlerConfigMap = WebCrawlerUtility.readConfigFile(configFileName)

    topic = ""
    while(topic is ""):
        print "Strategy configured is " + crawlerConfigMap["type"]
        topic = raw_input("Enter Topic for Crawling: ")

    # Get Seed Pages using BingSearchAPI. Output is BingWebSearch object
    seedPagesGetter = SeedPagesGetter(
        crawlerConfigMap["bingapikey"],
        topic, crawlerConfigMap["numseedstofetch"])
    seedPages = seedPagesGetter.getSeedPagesFromBing()

    if(len(seedPages) == 0):
        crawlerLogger.error("Failed to fetch Seed Pages: Run the program again on a different topic probably")

    # Run the Crawler
    isBFSStrategy = crawlerConfigMap["type"] == "bfs"
    fileName = crawlerConfigMap["outputfilename"] + "_" + crawlerConfigMap["type"]
    seedPages = [url.url for url in seedPages]
    thresholdForRelevance = float(crawlerConfigMap["thresholdforrelevance"])
    thresholdForLinksToParseOnOneSite = int(crawlerConfigMap["thresholdforlinkstoparseononesite"])

    crawler = Crawler(fileName, seedPages, topic, isBFSStrategy,
                      thresholdForRelevance, thresholdForLinksToParseOnOneSite,
                      int(crawlerConfigMap["totalpagestocrawl"]))
    crawler.run()
