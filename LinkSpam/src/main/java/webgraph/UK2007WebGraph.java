package webgraph;

import commons.CommonUtilities;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.webgraph.Transform;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UK2007WebGraph {
    private static final Logger LOGGER = Logger.getLogger(UK2007WebGraph.class.getName());
    private static final Properties CONF = CommonUtilities.getConfFile();

    private BVGraph ukDomainGraph;

    public UK2007WebGraph(String baseName) {
        String graphPath = CONF.getProperty("graph-path");

        try {
            this.ukDomainGraph = BVGraph.load(graphPath + baseName); // loading completing in memory
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException in reading BVGraph File\n" + e.getMessage());
        }
    }

    public BVGraph getUnderlyingGraph() { return this.ukDomainGraph; }

    public ImmutableGraph getTransposedGraph() throws IOException {
        if(this.ukDomainGraph == null)
            LOGGER.log(Level.SEVERE, "Load original Graph, before transposing it.");

        ProgressLogger logger = new ProgressLogger();
        ImmutableGraph transposedGraph = Transform.transposeOffline(this.ukDomainGraph, 50000, null, logger);
        return transposedGraph;
    }

    public static void saveGraphToMemory(String file, ImmutableGraph graph) throws IOException {
        ProgressLogger pl = new ProgressLogger();
        BVGraph.store(graph, file, pl);
    }


    public static void main(String[] args) throws IOException {
        System.out.println("Current Memory Avaiable - Before Loading Graph(MB) = " + (Runtime.getRuntime().freeMemory()/(1024*1024)));
        UK2007WebGraph datasetGraph = new UK2007WebGraph(CONF.getProperty("graph-basename"));
        BVGraph graph = datasetGraph.getUnderlyingGraph();
        System.out.println("Current Memory Avaiable - After Loading Graph(MB) = " + (Runtime.getRuntime().freeMemory()/(1024*1024)));

        ImmutableGraph transposedGraph = datasetGraph.getTransposedGraph();
        System.out.println("Current Memory Avaiable - After Transposing Graph(MB) = " + (Runtime.getRuntime().freeMemory()/(1024*1024)));

        System.out.println("Saving and Compressing Transposed Graph");
        UK2007WebGraph.saveGraphToMemory(CONF.getProperty("graph-path") +
                CONF.getProperty("transposed-graph-basename"), transposedGraph);
    }
}
