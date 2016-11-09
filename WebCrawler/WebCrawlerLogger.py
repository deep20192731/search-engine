import logging


logFormatter = logging.Formatter(
    "%(asctime)s [%(filename)s] [%(funcName)s] [%(levelname)s]" +
    "[%(lineno)d] %(message)s")
fileHandler = logging.FileHandler("crawler.log")
fileHandler.setFormatter(logFormatter)
fileHandler.setLevel(logging.WARN)

streamHandler = logging.StreamHandler()
streamHandler.setFormatter(logFormatter)
streamHandler.setLevel(logging.ERROR)

crawlerLogger = logging.getLogger(__name__)
crawlerLogger.addHandler(fileHandler)
crawlerLogger.addHandler(streamHandler)
