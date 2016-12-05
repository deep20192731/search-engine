'''
Dataset-Source = http://chato.cl/webspam/datasets/uk2007/
Domain = uk
Total-Pages = 105,896,555
Total-Hosts = 114,529

Hosts-File = uk-2007-05.hostnames.txt (Format:  Host-Id Host-Name)
HostGraph-File = uk-2007-05.hostgraph_weighted.graph-txt (Format: Host-Id:Number of outlinks from base-host(line-num) to this host)
'''

from CommonUtilities import CommonUtilities

def totalLines(file):
    counter = 0
    for line in file:
        counter += 1
    return counter

if __name__ == '__main__':
    CONFIG_FILE = CommonUtilities.readConfigFile()

    datasetFilePath = CONFIG_FILE["dataset-path"]

    hostsFileName = datasetFilePath + CONFIG_FILE["hosts-file"]
    hostsFile = open(hostsFileName)
    hosts = hostsFile.readlines()
    hosts = map(lambda host:host.replace("\n", ""), hosts)

    hostGraphFileName = datasetFilePath + CONFIG_FILE["host-graph-file"]
    hostGraphFile = open(hostGraphFileName)
    hostGraph = hostGraphFile.readlines()
    hostGraph = hostGraph[1:] # since first line = number of hosts (we know that already)

    '''
    Not a Good Idea to open a 11.8 GB file like this in Python. See if you want to do in Python, or you need to switch to Java
    urlFileName = datasetFilePath + CONFIG_FILE["url-file"]
    urlFile = open(urlFileName)
    urls = urlFile.readlines()
    print len(urls)
    '''

    # Need to shift to java, since more experience handling with larger files in Java.
    # Also, C++ and Java code is provided to read the compressed adjacency list using BV Format
    # http://chato.cl/webspam/datasets/uk2007/links/
