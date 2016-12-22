### Observations
1. When Trust Rank runs with training-file seeds, the node with highest page rank is the one with maximum trust rank
(10990, 0.003510992235524701)
(10990, 0.003506482668722062)
2. Trust Rank Run (Times = 1) -> In general Trust Rank Scores for Spam Pages is lowered down (on average by 4.50956680264e-06)
                                 For Non spam Pages, the trust rank goes significantly up from page-ranks (an average by 0.000132269081592)
                                 But if you compare with scale of Page Ranks and Trust Ranks they are very close
3. Anti Trust Rank (Times = 1) -> For spam pages takes the anti trust rank way higher than trust rank (0.01 and above..way more then other scales)
   Also less number of seed pages since total initial seed pages for spam is way less than non-spam pages
4. Decision Tree Classifier - 0.91 accuracy with only content features, because of the imbalance in training data so more towards non-spam
Below report confirms this <br/>
precision    recall  f1-score   support

          0       0.27      0.28      0.28       113
          1       0.96      0.95      0.95      1835

avg / total       0.92      0.91      0.92      1948
Kappa Score = 0.232994463921

Undersampling
precision    recall  f1-score   support

          0       0.00      0.00      0.00       113
          1       0.94      1.00      0.97      1835

avg / total       0.89      0.94      0.91      1948
Kappa Score = 0.0

Cost Sensitive Learning DT
precision    recall  f1-score   support

          0       0.13      0.63      0.21       113
          1       0.97      0.73      0.84      1835

avg / total       0.92      0.73      0.80      1948
Kappa Score = 0.12668897812

Random Forest Ensemble
 precision    recall  f1-score   support

          0       0.63      0.26      0.36       113
          1       0.96      0.99      0.97      1835

avg / total       0.94      0.95      0.94      1948
Kappa Score = 0.342718550392

Random Forest Ensemble with Link Features
precision    recall  f1-score   support

          0       0.20      0.07      0.10       122
          1       0.94      0.98      0.96      1933

avg / total       0.90      0.93      0.91      2055
Kappa Score = 0.071545721801

Random Forest With Combined Features
precision    recall  f1-score   support

          0       0.62      0.13      0.22       113
          1       0.95      1.00      0.97      1835

avg / total       0.93      0.95      0.93      1948
Kappa Score = 0.202775270413

### Approach
1. Page Rank on Hosts
2. Trust Rank on Hosts with Preference Vector = Training File Seeds
3. Anti-Trust Rank
4. Decision Trees on Content Features
3. Threshold = Max(PageRank/TrustRank) score - 0.0005 (Threshold Selection approach)

### Evaluations
1. Precision and Recall
2. May be we can see the fraction of pages that have high page ranks and that are also spam.
   Does Trust or any other measure help reduce these. A measure measuring this
3. As in anti-trust rank paper, seeing the precision at different levels of recall 

### Observations for Web Spam Problem
1. Spam Hosts frequently link to Non-Spam Hosts but vice versa is rare.

### Statistics of Features
1. Obvious Features
        #hostid         number_of_pages     length_of_hostname
count  114529.000000    114529.000000       114529.000000
mean    57264.000000       924.626557           22.555204
std     33061.818827      4668.431700            5.425224
min         0.000000         1.000000            8.000000
25%     28632.000000         4.000000           19.000000
50%     57264.000000        18.000000           22.000000
75%     85896.000000        94.000000           26.000000
max    114528.000000     50000.000000          134.000000

2. Training and Evaluation Set Statistics. This is the prevalence of each label in SET1 and SET2.

    SET1, given for training in the Web Spam Challenge 2008:
        3776 nonspam<br/>
        222 spam<br/>
        277 undecided<br/>

    SET2, held for testing in the Web Spam Challenge 2008:
        1933 nonspam<br/>
        122 spam<br/>
        149 undecided<br/>
