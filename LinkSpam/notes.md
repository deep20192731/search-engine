### ML Library in Java-  Notes
1. http://machinelearningmastery.com/java-machine-learning/
2. http://java-ml.sourceforge.net/
3. No Maven repository, only jar

### Web Graph Format
1. ASCIIGraphFormat - Adjacency Matrix representation
2. BVGraphFormat - Collection of 3 files (.graph, .offset and .properties). [3]
    1. loadMapped - Lazily loads the graph in memory (access should be slower)
    2. load - maps the complete graph in memory. May run out of heap-memory space

### Web-Graph Memory Time/Space Requirements
1. Loading Complete Graph in Memory - 1.24GB
2. Converting to Transposed Graph - ~6 mins
3. Saving a Graph to memory - 

### Approach- Notes
1. Seed set selection for any link-based approach is very critical [2]
2. If seed-set is mostly about sports --> spam pages are sports related [1]
    1. Attempt 1 = Trust Rank with random seed set selection (not taking into effect point-2 above)<br>
       Seed Set Selection = Same seed set as in the training set<br>
       Format Of Training/Test Files = hostid   label   spamicity   assessments

### Page-Rank Benchmarks [4]
LAW [5] library used for some implementations
1. Approximate iterations for convergence 40-50 (these per iteration times includes library loads too)
   Also, convergence takes a lot of time, limiting iterations to 10 produce good results with less delta
2. Naive Implementation (105 million nodes) - 12 mins/iteration (should be faster for further iterations, since we store outdegrees in a new array)
3. Simple Power Series - 1.5mins/iteration
4. Parallel Power Series - 1.23mins/iteration
5. Gaussian Siedel - 2.46 mins/iteration (leads to faster convergence)
6. <b>Parallel Gaussian Siedel - 1.85 mins/iteration or 36 mins till convergence(42 iterations)</b>

### Linear Quantization
1. Using 8 bits for linear quantization. Saw by trying some combinations. This was giving good-enough precision

#### References
[1] http://www.cse.lehigh.edu/~brian/pubs/2006/WWW/topical-trustrank.html
<br>[2] http://www.sciencedirect.com/science/article/pii/S0020025513000273
<br>[3] http://www.ics.uci.edu/~djp3/classes/2008_01_01_INF141/Materials/p595-boldi.pdf
<br>[4] http://ilpubs.stanford.edu:8090/386/1/1999-31.pdf
<br>[5] http://law.di.unimi.it/software.php