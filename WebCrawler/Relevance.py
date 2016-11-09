import math

space = " "


def _getNormalizedTermFrequency(text):
    termFreq = {}
    all_words = text.split(space)
    for word in all_words:
        if word in termFreq:
            termFreq[word] += 1.0
        else:
            termFreq[word] = 1.0

    # Normalize all the frequencies
    length = len(all_words)
    for key, value in termFreq.iteritems():
        termFreq[key] = float(value)/float(length)

    return termFreq


def _computeDotProduct(vec1, vec2):
    zipped = [m[0]*m[1] for m in zip(vec1, vec2)]
    return sum(zipped)


def _computeVectorModulus(vec):
    return math.sqrt(sum([m*m for m in vec]))


def _cosineSimilarity(textFreqMap, queryFreqMap):
    textFreqList = []
    queryFreqList = []

    for key, value in queryFreqMap.iteritems():
        queryFreqList.append(value)
        if key in textFreqMap:
            textFreqList.append(textFreqMap[key])
        else:
            textFreqList.append(0)

    dotProduct = _computeDotProduct(textFreqList, queryFreqList)
    queryModulus = _computeVectorModulus(queryFreqList)
    textModulus = _computeVectorModulus(textFreqList)

    cosineSimi = 0  # Returns 0 if any mod is 0
    if(queryModulus != 0 and textModulus != 0):
        cosineSimi = dotProduct / (queryModulus * textModulus)
    return cosineSimi


def getRelevanceScoreFromTextAndQuery(text, query):
    # Returns Cosine similarity between document and query
    textNorm = _getNormalizedTermFrequency(text.lower())
    queryNorm = _getNormalizedTermFrequency(query.lower())

    return _cosineSimilarity(textNorm, queryNorm)


def getRelevanceScoreFromLinkAndQuery(anchor, queryText):
    # If query terms in anchor than we assign more priority
    try:
        anchorFreq = _getNormalizedTermFrequency(anchor.lower())
        queryFreq = _getNormalizedTermFrequency(queryText.lower())
        return _cosineSimilarity(anchorFreq, queryFreq)
    except:
        return 0.0

def combineScoreFromTextAndLink(textScore, linkScore):
    # Weighting each by 0.4 and 0.6 ratio
    # TODO: Can experiment with these more
    return 0.7*textScore + 0.3*linkScore


def getRelScoreByCountingTerms(text, query):
    textNorm = _getNormalizedTermFrequency(text.lower())
    queryNorm = _getNormalizedTermFrequency(query.lower())

    sumOfFreq = 0.0
    for k, v in queryNorm.iteritems():
        if(k in textNorm):
            sumOfFreq += v

    return float(sumOfFreq)/float(len(queryNorm))
