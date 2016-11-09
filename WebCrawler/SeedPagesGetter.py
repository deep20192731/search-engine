from py_bing_search import PyBingWebSearch
from WebCrawlerLogger import crawlerLogger


class SeedPagesGetter:
    def __init__(self, apiKey, searchTerm, totalSeedPagesToFetch):
        self.apiKey = apiKey
        self.searchTerm = searchTerm
        self.totalSeedPagesToFetch = totalSeedPagesToFetch

    '''
    Returns a list of WebResults. Each has the following attributes

    self.title: title of the result
    self.url: the url of the result
    self.description: description for the result
    self.id: bing id for the page

    #Meta info:
    self.meta.uri: the search uri for bing
    self.meta.type: for the most part WebResult
    '''
    def getSeedPagesFromBing(self):
        try:
            bingWeb = PyBingWebSearch(self.apiKey, self.searchTerm)
            searchResults = bingWeb.search(limit=10, format="json")
            return searchResults
        except Exception as e:
            crawlerLogger.error("Failed to get seeds. Error:" + str(e))
