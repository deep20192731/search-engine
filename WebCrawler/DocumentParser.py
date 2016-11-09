import re
import urllib2
import posixpath
import html2text
import urlnorm
from WebCrawlerLogger import crawlerLogger
from urlparse import urlparse
from bs4 import BeautifulSoup
from reppy.cache import RobotsCache

# common file extensions that are not followed if they occur in links
IGNORED_EXTENSIONS = [
    # images
    'mng', 'pct', 'bmp', 'gif', 'jpg', 'jpeg', 'png', 'pst', 'psp', 'tif',
    'tiff', 'ai', 'drw', 'dxf', 'eps', 'ps', 'svg',
    # audio
    'mp3', 'wma', 'ogg', 'wav', 'ra', 'aac', 'mid', 'au', 'aiff',
    # video
    '3gp', 'asf', 'asx', 'avi', 'mov', 'mp4', 'mpg', 'qt', 'rm', 'swf', 'wmv',
    'm4a',
    # office suites
    'xls', 'xlsx', 'ppt', 'pptx', 'pps', 'doc', 'docx', 'odt', 'ods', 'odg',
    'odp',
    # other
    'css', 'pdf', 'exe', 'bin', 'rss', 'zip', 'rar',
]

mime_type_allowed = "text"

allowed_protocols = ["http", "https"]

delimiters = [",", ".", "!", "?", "/", "&", "-", ":", ";", "@", "'", "..."]

robots = RobotsCache()


class DocumentParser:

    @staticmethod
    def openAndReadURLContents(url):
        '''
            Opens and Return the HTML for the URL. Returns None for following
            1) If Robot Exclusion protocol is set for our agent
            2) After downloading if MIMETYPE is not "text/html"
            3) If response code is != 200
        '''
        htmlResponse = None
        pageSize = None
        responseCode = None
        try:
            # Only crawl if there is no robots exclusion set for us
            if(robots.allowed(str(url), 'my-agent')):
                response = urllib2.urlopen(url)

                # After downloading a page only explore it if mime type is text
                shouldWeExploreURL = response.info().maintype == mime_type_allowed
                responseCode = response.getcode()
                successInRetrieval = responseCode == 200

                if shouldWeExploreURL and successInRetrieval:
                    htmlResponse = response.read()
                    pageSize = len(htmlResponse)
                else:
                    if(not successInRetrieval):
                        return (None, None, responseCode)
                    crawlerLogger.warn("Mime Type not found for or Response Error code " + str(url))
            else:
                crawlerLogger.warn("Robot Exclusion there at: " + str(url))
        except urllib2.URLError:
            crawlerLogger.warn("Not able to open the URL - " + str(url))
        except Exception as e:
            crawlerLogger.warn("General Erorr: " + str(e))
        return (htmlResponse, pageSize, responseCode)

    @staticmethod
    def extractLinksFromHTML(htmlString):
        '''
            Returns all the links at htmlString
            Returns [] if htmlString == None
        '''
        # BeautifulSoup uses htmllib underneath
        linksAndAnchors = []
        if(htmlString is not None):
            soupObj = BeautifulSoup(htmlString, "html.parser")
            # For now only considering normal hyperlinks
            for link in soupObj.find_all("a"):
                linksAndAnchors.append((link.get("href"), "" if (len(link.contents) == 0 or link.contents[0] is None) else link.contents[0]))
        return linksAndAnchors

    @staticmethod
    def parseLinksForValidity(linksAndAnchors):
        '''
            Following rules are followed.
            1) isCGIScript = When link contains word "cgi"
            2) isRelativeURL = When Scheme is not in allowed_protocols
            3) extensionAllowed = When extension not in IGNORED_EXTENSIONS
        '''
        linksToRemove = []
        for link, anchor in linksAndAnchors:
            try:
                parseResult = urlparse(link)
                extension = posixpath.splitext(parseResult.path)[1].lower()

                isCGIScript = "cgi" in link or "CGI" in link
                isRelativeURL = parseResult.scheme not in allowed_protocols
                extensionNotAllowed = extension in IGNORED_EXTENSIONS

                if(isRelativeURL or isCGIScript or extensionNotAllowed):
                    linksToRemove.append((link, anchor))
                    crawlerLogger.warn("RelativeURL/CGIScript/ExtensionNotAllowed " + str(link))

            except Exception as err:
                linksToRemove.append((link, anchor))  # if NoneType then we remove it
                crawlerLogger.warn("Error while parsing URL " + str(type(link)) + " type found")

        for link in linksToRemove:
            linksAndAnchors.remove(link)
        return linksAndAnchors

    @staticmethod
    def removeDelimiters(s):
        for d in delimiters:
            ind = s.find(d)
            while ind != -1:
                s = s[:ind] + s[ind+1:]
                ind = s.find(d)
        return ' '.join(s.split())

    @staticmethod
    def visible(element):
        # Function Taken from https://www.quora.com/How-can-I-extract-only-text-data-from-HTML-pages
        if element.parent.name in ['style', 'script', '[document]', 'head', 'title']:
            return False
        elif re.match('<!--.*-->', str(element.encode('utf-8'))):
            return False
        return True

    @staticmethod
    def getTextFromHTML(html):
        parsedText = None
        try:
            h = html2text.HTML2Text()
            h.ignore_links = True
            h.ignore_images = True
            text = h.handle(html)
            text = text.decode('unicode_escape').encode('ascii', 'ignore')
            parsedText = DocumentParser.removeDelimiters(text)
        except UnicodeDecodeError as uniErr:
            crawlerLogger.warn("Unicode Error: " + str(uniErr))

        return parsedText

    @staticmethod
    def extractLinksAndTextFromURL(url):
        # Extract all valid links
        htmlResponse, pageSize, responseCode = DocumentParser.openAndReadURLContents(str(url))
        if(responseCode == 200):
            linksAndAnchors = DocumentParser.extractLinksFromHTML(htmlResponse)
            linksToExplore = DocumentParser.parseLinksForValidity(linksAndAnchors)

            # Extract all text
            if(len(linksToExplore) != 0):
                parsedText = DocumentParser.getTextFromHTML(htmlResponse)

                if(parsedText is not None):
                    return (parsedText, pageSize, responseCode, linksToExplore)
        return (None, pageSize, responseCode, [])
