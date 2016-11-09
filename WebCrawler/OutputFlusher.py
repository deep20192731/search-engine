import datetime
from CrawlerOutput import CrawlerOutput


class OutputFlusher:
    def __init__(self, outputFileName, numOfRecordsPerFile):
        self.outputFileName = outputFileName
        self.numOfRecordsPerFile = numOfRecordsPerFile
        self.crawledOutputs = []

    def _constructFileName(self):
        # Constructs the output file name with file creation datetime
        currentDateTime = datetime.datetime.now().strftime("%Y-%m-%d %H-%M-%S")
        return self.outputFileName + "_" + currentDateTime

    def _constructOutputLine(self, crawlerOp):
        '''
            Constructs output line in below format separated by \t
            url timeOfCrawl pageSize returnCode harvestRateUptil
        '''
        separator = "\t"
        newLine = "\n"

        return crawlerOp.url + separator + crawlerOp.timeOfCrawl + separator + str(crawlerOp.pageSize) + separator + str(crawlerOp.returnCode) + separator + str(crawlerOp.harvestRate*100) + newLine

    def createHeader(self, sep):
        return "URL" + sep + "TimeOfCrawl" + sep + "Page-Size" + sep + "Return-Code" + sep + "HarvestRate" + "\n"

    def createStatsHeader(self, sep):
        return "Total Files" + sep + "Total Time(minutes)" + sep + "Total Size" + sep + "Total Error Links" + "\n"

    def flushOutput(self, totalFiles, totalSize, totalTime, totalErrorLinks):
        '''
            This creates a new file for every flush.
            We flush (total_pages_to_crawl/num_of_op_logs) urls at once
        '''
        if(len(self.crawledOutputs) != 0):
            opFileName = self._constructFileName()
            opFile = open(opFileName, "w+")
            opFile.write(self.createHeader("\t"))
            for op in self.crawledOutputs:
                opFile.write(self._constructOutputLine(op))

            opFile.write("\n\n\nStatistics of the run\n")
            opFile.write(self.createStatsHeader("\t"))
            opFile.write(totalFiles + "\t" + totalTime + "\t" + totalSize + "\t" + totalErrorLinks + "\n")

            opFile.close()
        # self.crawledOutputs = []

    def addToOutputBuffer(self, crawledOutput):
        self.crawledOutputs.append(crawledOutput)
        '''
        # Flush to file if num of records reach threshold
        if(len(self.crawledOutputs) > self.numOfRecordsPerFile):
            self.flushOutput()
            self.crawledOutputs = []
        self.crawledOutputs.append(crawledOutput)
        '''
