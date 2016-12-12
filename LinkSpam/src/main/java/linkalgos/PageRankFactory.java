package linkalgos;

import commons.CommonUtilities;
import it.unimi.dsi.law.rank.*;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import java.io.*;
import java.util.Arrays;

public class PageRankFactory {
    private static final String OUTPUT_FILE_NAME = "page-rank";
    public static final double DECAY_FACTOR = PageRank.DEFAULT_THRESHOLD;
    public static final int ITERATIONS_NUM = 1;

    // Using the LAW library implementations
    public double[] optimalImplementation(BVGraph webGraph) throws IOException {
        PageRank pr = new PageRankPowerSeries(webGraph);
        pr.stepUntil(PageRank.or(new SpectralRanking.NormStoppingCriterion(DECAY_FACTOR),
                new SpectralRanking.IterationNumberStoppingCriterion(ITERATIONS_NUM)));
        return pr.rank;
    }

    public float[] naiveImplementation(BVGraph webGraph) {
        if(webGraph == null) return null;

        int totalNodes = webGraph.numNodes();

        float source[] = new float[totalNodes];
        float dest[] = new float[totalNodes];

        Arrays.fill(source, 1/totalNodes);
        Arrays.fill(dest, 0);

        int dummy = -1;
        int totalIterations = 0;
        while(totalIterations < ITERATIONS_NUM) {
            for(int i=0; i<totalNodes; i++) {
                System.out.println(i);
                int outdegree = webGraph.outdegree(i);
                if(outdegree == 0) continue;

                LazyIntIterator succ = webGraph.successors(i);
                while((dummy = succ.nextInt()) != -1) {
                    dest[dummy] += source[i]/outdegree;
                }
            }

            // Dampening Factor
            for(int i=0; i<totalNodes; i++) { dest[i] = (float) (DECAY_FACTOR*dest[i] + (1 - DECAY_FACTOR)/totalNodes); }

            totalIterations++;
        }

        return dest;
    }

    public static void main(String[] args) throws IOException {
        PageRankFactory pr = new PageRankFactory();
        String fileName = CommonUtilities.getConfFile().getProperty("output-path") + OUTPUT_FILE_NAME;

        // Computation of Page Ranks
        /*System.out.println("Starting Page Rank Computation...");
        double millis = System.currentTimeMillis();
        double[] pageRanks = pr.optimalImplementation(new UK2007WebGraph().getUnderlyingGraph());

        System.out.printf("Total Page Ranks = " + pageRanks.length);
        System.out.println("Page Rank Computation Done...in " + (System.currentTimeMillis() - millis));
        millis = System.currentTimeMillis();

        System.out.println("Saving page ranks to file...");
        // Compress and Store to a File
        double maxPageRank = CommonUtilities.getMaximum(pageRanks);
        System.out.println("Max Page Rank = " + maxPageRank);
        LinearQuantizer quantizer = new LinearQuantizer(maxPageRank, 2, false);
        ByteBuffer compressedBuffer = quantizer.compressArr(pageRanks);

        CommonUtilities.saveBytesToFile(fileName, compressedBuffer);

        System.out.println("Saved ranks to file...in " + (System.currentTimeMillis() - millis));

        System.out.println("Original = " + pageRanks[0]);
        System.out.println("Ori Pre = " + quantizer.getPrecision());
        System.out.println("Ori Bytes = " + quantizer.getBytes());*/
        // Calculating Accuracies
        //double[] d = CommonUtilities.readVectorFromFile(fileName);


    }
}