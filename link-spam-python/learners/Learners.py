from sklearn import tree
import numpy as np
from costcla.models import CostSensitiveDecisionTreeClassifier

class Learners():
    @staticmethod
    def learnDecisionTree(featureMatrix, outputLabels):
        clf = tree.DecisionTreeClassifier()
        clf = clf.fit(featureMatrix, outputLabels)
        return clf

    @staticmethod
    def learnCostSensitiveDecisionTree(featureMatrix, outputLabels, base, R):
        f = CostSensitiveDecisionTreeClassifier()
        costMatrix = np.zeros(shape = (len(outputLabels), 4))
        counter = 0
        for o in outputLabels:
            temp = []
            if(o == 1):
                temp.append(base)
                temp.append(base)
                temp.append(0)
                temp.append(0)
            else:
                temp.append(R*base)
                temp.append(R*base)
                temp.append(0)
                temp.append(0)
            costMatrix[counter] = temp
            counter += 1
        return f.fit(np.array(featureMatrix), np.array(outputLabels), costMatrix)
