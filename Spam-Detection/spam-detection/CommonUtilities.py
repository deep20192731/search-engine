import ConfigParser

class CommonUtilities:
    @staticmethod
    # Reads the 'ini' config file and returns key-values as Map
    def readConfigFile():
        config = ConfigParser.ConfigParser()
        config.read("common.ini")

        configMap = {}
        for section in config.sections():
            sectionOptions = config.options(section)
            for option in sectionOptions:
                configMap[option] = config.get(section, option)

        return configMap