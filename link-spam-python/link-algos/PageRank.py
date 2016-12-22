import networkx as nx
import pickle

class PageRank:
    def __init__(self, graph):
        self.graph = graph

    def pageRank(self, biasingVector, errorDelta = 0.0001, maxIterations = 1000, dampingFactor = 0.85):
        return nx.pagerank(self.graph, dampingFactor, biasingVector, maxIterations, errorDelta)

    def runPageRankOnTranspose(self, biasingVector, errorDelta = 0.0001, maxIterations = 1000, dampingFactor = 0.85):
        reverseGraph = self.getReverseGraph(False)
        return nx.pagerank(reverseGraph, dampingFactor, biasingVector, maxIterations, errorDelta)


    def getReverseGraph(self, copy):
        return self.graph.reverse(copy)

    def savePageRanksToDisk(self, filename, ranks):
        with open(filename, 'wb') as handle:
            pickle.dump(ranks, handle, protocol=pickle.HIGHEST_PROTOCOL)

    def loadPageRanksFromDisk(self, filename):
        with open(filename, 'rb') as handle:
            b = pickle.load(handle)
            return b

