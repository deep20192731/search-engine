import urlnorm
from WebCrawlerLogger import crawlerLogger
from heapq import heapify, heappush, heappop


class SimpleQueueWithDictionary:
    def __init__(self):
        self._linkDict = {}
        self._queue = []

    def size(self):
        return len(self._queue)

    def empty(self):
        return len(self._queue) == 0

    def returnURLSAsList(self):
        return self._queue

    def enqueueItems(self, urls):
        for url in urls:
            self.enqueue(url)

    def enqueue(self, url, *args):
        if(url not in self._linkDict):
            self._linkDict[url] = False
            self._queue.append(url)

    def dequeue(self):
        url = self._queue[0]
        self._linkDict[url] = True
        del self._queue[0]
        return url


class PriorityQueueWithDictionary(SimpleQueueWithDictionary):
    def __init__(self):
        SimpleQueueWithDictionary.__init__(self)

    def _buildHeap(self):
        # While building we use negative weights to make MAX-PQ
        allEme = [((-p, e), k) for k, (p, e) in self._linkDict.iteritems()]
        self._queue = []

        for (p, e), url in allEme:
            if(not e):
                self._queue.append(((p, e), url))
        heapify(self._queue)

    def dequeue(self):
        # Get the url. And pops it [0] is priority and [1] is URL
        (priority, explored), url = self._queue[0]
        heappop(self._queue)
        # Shld not delete from dictionary
        # del self._linkDict[url]
        priority, explored = self._linkDict[url]
        self._linkDict[url] = (priority, True)

        return url

    def enqueueItems(self, urls):
        # Add many items at once with max priority
        defaultPriority = 1.0
        for url in urls:
            self.enqueue(url, defaultPriority)

    def enqueue(self, url, *args):
        # We add explored bool too. Since links that
        # are not explored only can average prev scores
        # normalizedURL = url
        if(len(args) != 1):
            crawlerLogger.error("Required was Priority but more args supplied")

        priority = args[0]
        try:
            normalizedURL = urlnorm.norm(url)

            if(normalizedURL not in self._linkDict):
                self._linkDict[normalizedURL] = (priority, False)
            else:
                # Average the two scores if found
                prevPriority, explored = self._linkDict[normalizedURL]
                if(not explored):
                    self._linkDict[normalizedURL] = ((prevPriority+priority)/2, False)
        except Exception as e:
            crawlerLogger.warn("Normalization Issues. Not Enqueing " + url)
        self._buildHeap()
