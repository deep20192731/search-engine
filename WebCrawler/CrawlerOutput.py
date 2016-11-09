import datetime


class CrawlerOutput:
    def __init__(self, url, pageSize, returnCode, harvestRate):
        self.url = url
        self.timeOfCrawl = datetime.datetime.now().strftime("%Y-%m-%d %H-%M-%S")
        self.pageSize = pageSize
        self.returnCode = returnCode
        self.harvestRate = harvestRate  # This is at page-crawl moment
