import ConfigParser
import networkx as nx
import collections
import numpy as np
import matplotlib.pyplot as plt

class Commons:
    @staticmethod
    # Reads the 'ini' config file and returns key-values as Map
    def readConfigFile():
        config = ConfigParser.ConfigParser()
        config.read("../commons/properties.ini")

        configMap = {}
        for section in config.sections():
            sectionOptions = config.options(section)
            for option in sectionOptions:
                configMap[option] = config.get(section, option)

        return configMap

    @staticmethod
    # Constructs a NetworkX Graph
    def constructGraph(hostGraphFileName):
        hostGraphFile = open(hostGraphFileName)
        hostGraph = hostGraphFile.readlines()
        hostGraph = map(lambda links: links.replace("\n", ""), hostGraph) # trim end-line characters

        numNodes = int(hostGraph[0])
        hostGraph = hostGraph[1:]  # since first line = number of hosts (we know that already)

        graph = nx.DiGraph() # NetworkX Directed Graph

        for nodeNum in range(0, numNodes): # Add Nodes in the Graph
            graph.add_node(nodeNum)

        for nodeNum in range(0, numNodes): # Add Edges in the Graph
            links = hostGraph[nodeNum]
            if(len(links) == 0): continue # not considering nodes with 0 outdegree

            links = links.split(" ")

            try:
                links = map(lambda link: int(link.split(":")[0]), links)
            except ValueError:
                print "OOpss..Error occured while creating the graph"

            for link in links:
                graph.add_edge(nodeNum, link)

        hostGraphFile.close()
        return graph

    @staticmethod
    # Construct Preference Vector for Link-Algoritms (if returnSpam then return spamHosts)
    def getNonSpamLabels(filename, returnSpam, numNodes):
        f = open(filename)
        lines = f.readlines()

        lines = map(lambda line: line.replace("\n", ""), lines)

        hosts = set()

        str = "nonspam"
        if(returnSpam): str = "spam"

        for line in lines:
            line = line.split(" ")
            assert(len(line) == 4) # Following the pattern specified
            if(line[1] == str): hosts.add(int(line[0]))

        labelsMap = {}
        for node in range(0, numNodes):
            if(node in hosts): labelsMap[node] = 1.0 # normalization is done when running page-rank
            else: labelsMap[node] = 0.0

        f.close()
        return labelsMap

    @staticmethod
    # Get Dictionary of Spam/Non-Spam Labels (Redundant with previous function though)
    # True if Node is Spam else False
    def getLabelsDictionary(filename, numNodes):
        f = open(filename)
        lines = f.readlines()

        lines = map(lambda line: line.replace("\n", ""), lines)

        labelsMap = {}
        for line in lines:
            line = line.split(" ")
            assert (len(line) == 4)  # Following the pattern specified
            if (line[1] == "nonspam"): labelsMap[int(line[0])] = False
            elif (line[1] == "spam"): labelsMap[int(line[0])] = True

        f.close()
        return collections.OrderedDict(sorted(labelsMap.items()))

    @staticmethod
    # Plot of given feature w.r.t spam and non-spam
    def plotFeatureForBothLabels(spamMap, feature):
        # Plot for Spam Pages
        spam = []
        nonspam = []
        for key in spamMap:
            try:
                if (spamMap[key] == True):
                    spam.append(feature[key])
                else:
                    nonspam.append(feature[key])
            except KeyError:
                continue

        plt.plot(np.array(spam), color="r")
        plt.plot(np.array(nonspam), color = "b")
        plt.show()
