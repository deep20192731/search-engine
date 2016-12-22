from Commons import Commons
from Learners import Learners
from PageRank import PageRank
import networkx as nx
import pandas as pd
from sklearn import tree
from sklearn.metrics import average_precision_score
from sklearn.metrics import recall_score
from sklearn.metrics import f1_score
from sklearn.ensemble import RandomForestClassifier
import numpy as np
import sklearn.metrics as metrics
import matplotlib.pyplot as plt
from sklearn.metrics import classification_report
from sklearn.metrics import cohen_kappa_score
from costcla.models import CostSensitiveDecisionTreeClassifier

CONFIG_FILE = Commons.readConfigFile()
datasetFilePath = CONFIG_FILE["dataset-path"]
outputFilePath = CONFIG_FILE["output-path"]
hostGraphFileName = datasetFilePath + CONFIG_FILE["host-graph-file"]

NUM_NODES = 114529 # from analysis before
SPAM_LABEL = 0
NON_SPAM_LABEL = 1


'''
 1. Construct Graph From File
'''
graph = Commons.constructGraph(hostGraphFileName)
pr = PageRank(graph)


'''
 1. Run Page Rank
 2. Pickle Page-Rank Dictionary

ranks = pr.pageRank(None)
filename = outputFilePath + CONFIG_FILE["page-rank-file"]
# pr.savePageRanksToDisk(filename, ranks)
'''


'''
TRUST RANK
 1. Extract Seeds from Training File to be used as Preference Vector (1 if node is non-spam else 0)
    Normalization of this vector done inside page-rank comoutation
 2. Run Page Rank with Preference Vector = Trust Rank (with Dampening and Splitting)
 3. Pickle Trust Ranks


trainingFileName = datasetFilePath + CONFIG_FILE["training-file"]
biasedVector = Commons.getNonSpamLabels(trainingFileName, False, graph.number_of_nodes())

trustRanks = pr.pageRank(biasedVector)
pr.savePageRanksToDisk(outputFilePath + CONFIG_FILE["trust-rank-file"] + "-1", trustRanks)
'''

'''
ANTI-TRUST RANK
 1. Extract Seeds from Training File to be used as Preference Vector (1 if node is spam else 0)
    Normalization of this vector done inside page-rank comoutation
 2. Get transpose of the web graph since anti trust is propagated in reverse direction
 3. Run Page Rank with Preference Vector = Anti Trust Rank (with Dampening and Splitting)
 4. Pickle Trust Ranks


trainingFileName = datasetFilePath + CONFIG_FILE["training-file"]
biasedVector = Commons.getNonSpamLabels(trainingFileName, True, NUM_NODES)

antiTrustRanks = pr.runPageRankOnTranspose(biasedVector)
pr.savePageRanksToDisk(outputFilePath + CONFIG_FILE["anti-trust-rank-file"] + "-1", antiTrustRanks)
'''

'''
 Classifier/Learner with Page Content Features
 a. Decision Trees
 b. Undersampling with Decision Trees
 c. Cost Sensitive with Decision Trees
 d. Random Forests Ensemble
 Steps
 1. Load Features
 2. Preprocessing of Features
 3. Run Classifier
 4. Save Results to file
'''

spamMap = Commons.getLabelsDictionary(datasetFilePath + CONFIG_FILE["training-file"], NUM_NODES)

contentFeatures = pd.read_csv(datasetFilePath + CONFIG_FILE["content-features-file"])
linkFeatures = pd.read_csv(datasetFilePath + CONFIG_FILE["link-features-file"])

# Make Training Data
outputLabels = []
featureMatrix = []

for key in spamMap:
    try:
        vec = contentFeatures.loc[contentFeatures['#hostid'] == key]
        del vec['#hostid']
        del vec['hostname']
        vec = vec.as_matrix()[0]
        featureMatrix.append(vec)
    except IndexError: # not for all hosts content features were calculated
        continue

    if(spamMap[key] == True): outputLabels.append(SPAM_LABEL)
    else: outputLabels.append(NON_SPAM_LABEL)

featureMatrixForLinkFeatures = []
outputLabelsForLinkFeatures = []

for key in spamMap:
    try:
        vec = linkFeatures.loc[linkFeatures['#hostid'] == key]
        vec1 = contentFeatures.loc[contentFeatures['#hostid'] == key]
        del vec['#hostid']
        del vec['hostname']
        del vec1['#hostid']
        del vec1['hostname']
        vec = vec.as_matrix()[0]
        vec1 = vec1.as_matrix()[0]
        featureMatrixForLinkFeatures.append(np.concatenate([vec, vec1]))
    except IndexError: # not for all hosts content features were calculated
        continue

    if(spamMap[key] == True): outputLabelsForLinkFeatures.append(SPAM_LABEL)
    else: outputLabelsForLinkFeatures.append(NON_SPAM_LABEL)

# Make Test Data
testSpamMap = Commons.getLabelsDictionary(datasetFilePath + CONFIG_FILE["test-file"], NUM_NODES)

testOutputLabels = []
testFeatureMatrix = []

for key in testSpamMap:
    try:
        vec = contentFeatures.loc[contentFeatures['#hostid'] == key]
        del vec['#hostid']
        del vec['hostname']
        vec = vec.as_matrix()[0]
        testFeatureMatrix.append(vec)
    except IndexError: # not for all hosts content features were calculated
        continue

    if(testSpamMap[key] == True): testOutputLabels.append(SPAM_LABEL)
    else: testOutputLabels.append(NON_SPAM_LABEL)

testOutputLabelsForLinkFeatures = []
testOutputMatrixForLinkFeatures = []

for key in testSpamMap:
    try:
        vec = linkFeatures.loc[linkFeatures['#hostid'] == key]
        vec1 = contentFeatures.loc[contentFeatures['#hostid'] == key]
        del vec['#hostid']
        del vec['hostname']
        del vec1['#hostid']
        del vec1['hostname']
        vec = vec.as_matrix()[0]
        vec1 = vec1.as_matrix()[0]
        testOutputMatrixForLinkFeatures.append(np.concatenate([vec, vec1]))
    except IndexError:
        continue

    if(testSpamMap[key] == True): testOutputLabelsForLinkFeatures.append(SPAM_LABEL)
    else: testOutputLabelsForLinkFeatures.append(NON_SPAM_LABEL)

# Make Features for all nodes (not all host ids have content-features)
trimmedHostIds = []
for i in contentFeatures['#hostid']:
    trimmedHostIds.append(i)

trimmedHostIdsForLinkFeatures = []
for i in range(1, len(linkFeatures)): trimmedHostIdsForLinkFeatures.append(i)

del contentFeatures['#hostid']
del contentFeatures['hostname']

del linkFeatures['#hostid']
del linkFeatures['hostname']

'''
print "For Normal DT"
# Run Classifier
dt = Learners.learnDecisionTree(featureMatrix, outputLabels)
predictedClasses = dt.predict(testFeatureMatrix)

print average_precision_score(testOutputLabels, predictedClasses)
print recall_score(testOutputLabels, predictedClasses)
print f1_score(testOutputLabels, predictedClasses)
print classification_report(testOutputLabels, predictedClasses)
print cohen_kappa_score(testOutputLabels, predictedClasses)
'''

'''
print "\nUndersampling for DT"
indices = []
num = 500
counter = 0
for i in range(1, len(outputLabels)):
    if(outputLabels[i] == NON_SPAM_LABEL and counter <= num):
        indices.append(i)
        counter += 1

for i in outputLabels:
    if(i == SPAM_LABEL):
        indices.append(i)

trimmedFeatureMat = [featureMatrix[i] for i in indices]
trimmedOutputLabels = [outputLabels[i] for i in indices]

dt = Learners.learnDecisionTree(trimmedFeatureMat, trimmedOutputLabels)
predictedClasses = dt.predict(testFeatureMatrix)

print average_precision_score(testOutputLabels, predictedClasses)
print recall_score(testOutputLabels, predictedClasses)
print f1_score(testOutputLabels, predictedClasses)
print classification_report(testOutputLabels, predictedClasses)
print cohen_kappa_score(testOutputLabels, predictedClasses)
'''

'''
print "\nCost-Sensitive DT"
csDT = Learners.learnCostSensitiveDecisionTree(featureMatrix, outputLabels, 1000, 20)
csDTPred = csDT.predict(np.array(testFeatureMatrix))
print average_precision_score(testOutputLabels, csDTPred)
print recall_score(testOutputLabels, csDTPred)
print f1_score(testOutputLabels, csDTPred)
print classification_report(testOutputLabels, csDTPred)
print cohen_kappa_score(testOutputLabels, csDTPred)
'''


print "Random Forest Ensemble for Unbalanced Classification"
clf = RandomForestClassifier(n_estimators=10, class_weight={1:1, 0:20})

clf = clf.fit(featureMatrixForLinkFeatures, outputLabelsForLinkFeatures)
rFPred = clf.predict(testOutputMatrixForLinkFeatures)

# predictionOnAll = clf.predict_proba(contentFeatures)
print average_precision_score(testOutputLabelsForLinkFeatures, rFPred)
print recall_score(testOutputLabelsForLinkFeatures, rFPred)
print f1_score(testOutputLabelsForLinkFeatures, rFPred)
print classification_report(testOutputLabelsForLinkFeatures, rFPred)
print cohen_kappa_score(testOutputLabelsForLinkFeatures, rFPred)

'''
# Get Host Ids
threshold = 0.95
confidenceBackedSpamHosts = []
for i in range(0, len(predictionOnAll)):
    if(predictionOnAll[i][0] > threshold):
        confidenceBackedSpamHosts.append(trimmedHostIds[i])

print len(confidenceBackedSpamHosts)
# Running Anti Trust with Classifications from Random Forest as Seeds
trainingFileName = datasetFilePath + CONFIG_FILE["training-file"]

biasedVector = {}
for node in range(0, NUM_NODES):
    if(node in confidenceBackedSpamHosts): biasedVector[node] = 1.0 # normalization is done when running page-rank
    else: biasedVector[node] = 0.0

trainingFileName = datasetFilePath + CONFIG_FILE["training-file"]
biasedVector2 = Commons.getNonSpamLabels(trainingFileName, True, NUM_NODES)

for key in biasedVector2:
    if(key not in biasedVector): biasedVector[key] = biasedVector2[key]

antiTrustRanks = pr.runPageRankOnTranspose(biasedVector)
pr.savePageRanksToDisk(outputFilePath + CONFIG_FILE["anti-trust-rank-file"] + "-2", antiTrustRanks)
'''


'''
 Analysis of Page Rank, Trust Ranks, Anti Trust Ranks, Spam Ranks Distribution
 1. Get Spam/Non Spam Labels from Training Set
 2. Load the Page and Trust Ranks from Disk
 3. Plot

trainingFileName = datasetFilePath + CONFIG_FILE["training-file"]
pageRankFileName = outputFilePath + CONFIG_FILE["page-rank-file"]
trustRankFileName = outputFilePath + CONFIG_FILE["trust-rank-file"] + "-1"
antiTrustFileName = outputFilePath + CONFIG_FILE["anti-trust-rank-file"] + "-1"

spamMap = Commons.getLabelsDictionary(trainingFileName, NUM_NODES)

pr = PageRank(None)
pageRanks = pd.Series(pr.loadPageRanksFromDisk(pageRankFileName))
trustRanks = pd.Series(pr.loadPageRanksFromDisk(trustRankFileName))
antiTrustRanks = pd.Series(pr.loadPageRanksFromDisk(antiTrustFileName))

# plt.plot(range(NUM_NODES), antiTrustRanks.values)

# Plot for Spam Pages
trustRankVec = []
pageRankVec = []
antiTrustRankVec = []
for key in spamMap:
    if(spamMap[key] == False):
        antiTrustRankVec.append(antiTrustRanks[key])
        trustRankVec.append(trustRanks[key])
        pageRankVec.append(pageRanks[key])

#plt.plot(spamX, np.array(spamY), color = "b")
plt.plot(antiTrustRankVec, color = "g")
plt.show()
'''

'''
STILL PENDING....
 Accuracy Calculation (Precision and Recall)
 1. Get Spam/Non Spam Labels from Test Set
 2. Select Threshold (how is defined in notes)
 3.


testFileName = datasetFilePath + CONFIG_FILE["test-file"]
pageRankFileName = outputFilePath + CONFIG_FILE["page-rank-file"]
trustRankFileName = outputFilePath + CONFIG_FILE["trust-rank-file"] + "-1"
antitrustFileName = outputFilePath + CONFIG_FILE["anti-trust-rank-file"] + "-1"

spamMap = Commons.getLabelsDictionary(testFileName, NUM_NODES)

pr = PageRank(None)
pageRanks = pd.Series(pr.loadPageRanksFromDisk(pageRankFileName))
trustRanks = pd.Series(pr.loadPageRanksFromDisk(trustRankFileName))
antiTrustRanks = pd.Series(pr.loadPageRanksFromDisk(antitrustFileName))

thresholdForPageRank = 0.00001
thresholdForTrustRank = 0.00014
thresholdForAntiTrustRank = 0.01 # above this is spam

# correctlySpamClassied = 0.0
# totalSpam = 0.0
pageRanksConfidence = []
trustRanksConfidence = []
antiTrustConfidence = []
labels = []
for key in spamMap:
    if(spamMap[key] == True): labels.append(SPAM_LABEL)
    else: labels.append(NON_SPAM_LABEL)
    pageRanksConfidence.append(pageRanks[key])
    trustRanksConfidence.append(trustRanks[key])
    antiTrustConfidence.append(antiTrustRanks[key])

labels = np.array(labels)
trustRanksConfidence = np.array(trustRanksConfidence)
pageRanksConfidence = np.array(pageRanksConfidence)
antiTrustConfidence = np.array(antiTrustConfidence)

prLabels = map(lambda x:1 if x > thresholdForPageRank else 0, pageRanksConfidence)
trLabels = map(lambda x:1 if x > thresholdForTrustRank else 0, trustRanksConfidence)
atLabels = map(lambda x:1 if x < thresholdForAntiTrustRank else 0, antiTrustConfidence)

fpr, tpr, thresholds = metrics.roc_curve(labels, prLabels)
print metrics.auc(fpr, tpr)

fpr, tpr, thresholds = metrics.roc_curve(labels, atLabels)
print metrics.auc(fpr, tpr)

plt.title('Receiver Operating Characteristic')
plt.plot(fpr, tpr, 'b',
label='AUC = %0.2f'% metrics.auc(fpr, tpr))
plt.legend(loc='lower right')
plt.plot([0,1],[0,1],'r--')
plt.xlim([-0.1,1.2])
plt.ylim([-0.1,1.2])
plt.ylabel('True Positive Rate')
plt.xlabel('False Positive Rate')
plt.show()

fpr, tpr, thresholds = metrics.roc_curve(labels, trLabels)
print metrics.auc(fpr, tpr)
'''


'''
 Features Analysis w.r.t Spam/Non-Spam
 1. Load features as Pandas Dataframe


spamMap = Commons.getLabelsDictionary(datasetFilePath + CONFIG_FILE["training-file"], NUM_NODES)

obviousFeatures = pd.read_csv(datasetFilePath + CONFIG_FILE["obvious-features-file"])
# Commons.plotFeatureForBothLabels(spamMap, obviousFeatures['length_of_hostname'])

contentFeatures = pd.read_csv(datasetFilePath + CONFIG_FILE["content-features-file"])
'''

'''
Graph Plot
pos = nx.circular_layout(graph)
nx.draw_circular(graph)
labels = {i : i + 1 for i in graph.nodes()}
nx.draw_networkx_labels(graph, pos, labels, font_size=15)
plt.show()
'''






