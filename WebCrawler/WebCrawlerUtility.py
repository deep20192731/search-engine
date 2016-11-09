import ConfigParser
import datetime


class WebCrawlerUtility:
    # Reads the 'ini' config file and returns key-values as Map
    @staticmethod
    def readConfigFile(configFileName):
        config = ConfigParser.ConfigParser()
        config.read(configFileName)

        configMap = {}
        for section in config.sections():
            sectionOptions = config.options(section)
            for option in sectionOptions:
                configMap[option] = config.get(section, option)

        return configMap

    # Creates CRAWL and SEED output files according to currentDateTime
    @staticmethod
    def getOutputFile(crawlOutFileName, seedOutFileName):
        currentDateTime = datetime.datetime.now().strftime("%Y-%m-%d %H-%M-%S")

        crawlOutputFile = file(crawlOutFileName + "_" + currentDateTime, 'a')
        seedOutputFile = file(seedOutFileName + "_" + currentDateTime, 'a')

        return (crawlOutputFile, seedOutputFile)
