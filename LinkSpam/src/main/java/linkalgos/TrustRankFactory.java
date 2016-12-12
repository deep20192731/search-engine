package linkalgos;

import commons.CommonUtilities;
import commons.CommonsConstants;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.law.rank.PageRank;
import it.unimi.dsi.law.rank.PageRankPowerSeries;
import it.unimi.dsi.law.rank.SpectralRanking;
import it.unimi.dsi.webgraph.BVGraph;

import java.io.*;
import java.util.*;

public class TrustRankFactory {
    private static final String OUTPUT_FILE_NAME = "trust-rank";

    private static String NORMAL_PAGE = "nonspam";
    private static String SPAM_PAGE = "spam";
    private static String UNDECIDED_PAGE = "undecided";

    private static double GOOD_SEED_PAGE_SCORE = 1.0;
    private static double UNDECIDED_OR_BAD_PAGE_SCORE = 0.0;

    // Selects seeds from given file-name. File given can be either training or test file provided by WEBSPAM commite.
    public List<Integer> getSeedsFromWebSpamDatasetFile(String seedFile) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(seedFile));
        List<Integer> goodSeeds = new ArrayList<Integer>(); // all pages that are not spam in training file
        String line;

        while((line = fileReader.readLine()) != null) {
            String[] parts = line.split(CommonsConstants.SPACE);
            assert(parts.length == 4);

            if(parts[1].equals(NORMAL_PAGE)) goodSeeds.add(Integer.parseInt(parts[0]));
        }
        fileReader.close();

        Collections.sort(goodSeeds); // so that we can do binary search on this
        return goodSeeds;
    }

    // Trust Rank = Trust Splitting + Dampening (20 iterations)
    public double[] getTrustRank(List<Integer> goodSeeds, BVGraph webGraph) throws IOException {
        int numNodes = webGraph.numNodes();
        int totalGoodSeeds = goodSeeds.size();

        // Construct Preference Vector
        DoubleList preferenceVector = new DoubleArrayList();
        for(int i=0; i<numNodes; i++) {
            int index = Collections.binarySearch(goodSeeds, i);
            if(index >= 0) preferenceVector.add(GOOD_SEED_PAGE_SCORE/totalGoodSeeds); // making vector stochastic
            else preferenceVector.add(UNDECIDED_OR_BAD_PAGE_SCORE);
        }

        PageRank pr = new PageRankPowerSeries(webGraph);
        pr.stronglyPreferential = true;
        pr.preference = preferenceVector;

        pr.stepUntil(PageRank.or(new SpectralRanking.NormStoppingCriterion(PageRankFactory.DECAY_FACTOR),
                new SpectralRanking.IterationNumberStoppingCriterion(PageRankFactory.ITERATIONS_NUM)));
        return pr.rank;
    }

    public Map<Integer, Boolean> getTestLabelsFromDataset(String filename) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(filename));
        Map<Integer, Boolean> labelsMap = new HashMap<Integer, Boolean>();

        String line;
        while((line = fileReader.readLine()) != null) {
            String[] parts = line.split(CommonsConstants.SPACE);
            assert(parts.length == 4);

            if(!parts[1].equals(UNDECIDED_PAGE)) {
                boolean isSpamPage = parts[1].equals(SPAM_PAGE);
                int page = Integer.parseInt(parts[0]);
                if(!labelsMap.containsKey(page)) {
                    if(isSpamPage) labelsMap.put(page, false);
                    else labelsMap.put(page, true);
                }
            }
        }
        fileReader.close();
        return labelsMap;
    }

    public static void main(String[] args) throws IOException {
        String trainingFile = CommonUtilities.getConfFile().getProperty("dataset-path") +
                CommonUtilities.getConfFile().getProperty("training-file");
        String testFile = CommonUtilities.getConfFile().getProperty("dataset-path") +
                CommonUtilities.getConfFile().getProperty("test-file");

        String fileName = CommonUtilities.getConfFile().getProperty("output-path") + OUTPUT_FILE_NAME;
        TrustRankFactory tf = new TrustRankFactory();

        // Computing the Trust Rank Scores
        /*System.out.println("Starting Trust Rank Computation...");
        double millis = System.currentTimeMillis();

        double[] trustRankScores = tf.getTrustRank(tf.getSeedsFromWebSpamDatasetFile(trainingFile), new UK2007WebGraph().getUnderlyingGraph());

        System.out.println("Random = " + trustRankScores[100132]);
        System.out.println("Trust Rank Computation Done...in " + (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();

        System.out.println("Saving trust ranks to file...");

        // Compress and Store to a File
        LinearQuantizer quantizer = new LinearQuantizer(CommonUtilities.getMaximum(trustRankScores), 1, false);
        ByteBuffer compressedBuffer = quantizer.compressArr(trustRankScores);

        String fileName = CommonUtilities.getConfFile().getProperty("output-path") + OUTPUT_FILE_NAME;
        CommonUtilities.saveBytesToFile(fileName, compressedBuffer);

        System.out.println("Saved ranks to file...in " + (System.currentTimeMillis() - millis));*/

        // Getting Accuracy Measures
        //Map<Integer, Boolean> map = tf.getTestLabelsFromDataset(testFile);
        //List<Double> d = CommonUtilities.readVectorFromFile(fileName);
        //System.out.println(d.size());
        double d = 5.377180752603636E-4;
        double p = d/255;

        int intRepresentation = (int) Math.ceil(10.6229153209385465E-8/p);
        System.out.println(intRepresentation);
    }
}
