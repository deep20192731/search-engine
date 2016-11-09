package websearch.queryprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.processing.RoundEnvironment;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

import websearch.commons.CommonsConstants;
import websearch.commons.DiskFileHelperMethods;
import websearch.index.URLTableEntry;
import websearch.intermediate.IntermediatePostingsConstants;
import websearch.queryprocessor.MemoryBlock.Posting;

public class QueryProcessor {
	private static final Properties CONFIG = getRawPostingsConfig();
	private static final Logger LOGGER = Logger.getLogger(QueryProcessor.class.getName());
	
	private static final String LEXICON_FILE = "lexicon";
	private static final String URLTABLE_FILE = "urltable";
	private static final String INVERTED_INDEX_FILE_NAME = "inverted-index";
	
	// Data structures required for query-processing
	public static List<String> VOCAB; // representing as list of string since loads faster with a litte overhead of binary search a word
	public static Cache CACHE;
	public static List<URLTableEntry> URLTABLE;
	
	public static long CHUNK_SIZE = Long.parseLong(CONFIG.getProperty("chunkSize"));
	public static int TOTAL_REL_RESULTS = 10;
	
	public static long AVG_DOC_LENGTH;
	public static RandomAccessFile INVERTED_INDEX_FILE;
	
	public static class DocumentScore implements Comparable<DocumentScore> {
		public int documentId;
		public Double bm25Score;
		
		public int[] freqs;
		public String[] words;
		
		public DocumentScore(int d, double score, int[] fres, String[] wrds) {
			this.documentId = d;
			this.bm25Score = score;
			this.freqs = fres;
			this.words = wrds;
		}
		
		@Override
		public int compareTo(DocumentScore o) { return this.bm25Score.compareTo(o.bm25Score); }
	}
	
	private static Properties getRawPostingsConfig() {
		Properties config = new Properties();
		try {
			config.load(QueryProcessor.class.
					getResourceAsStream("processor.properties"));
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, "Not able to open the config file " +
					IntermediatePostingsConstants.CONFIG_FILENAME);
		}
		return config;
	}
	
	public QueryProcessor() throws FileNotFoundException {
		INVERTED_INDEX_FILE = new RandomAccessFile(CONFIG.getProperty("inputpath") + "/" +
				INVERTED_INDEX_FILE_NAME, "rw");
	}
	
	// 1. Initialize CacheTO
	// 2. Initialize Lexicon
	// 3. Initialize URL Table
	public static void inititalizeQueryProcessor() throws IOException {
		long millis = System.currentTimeMillis();
		
		File urlTableFile = new File(CONFIG.getProperty("inputpath") + "/" + URLTABLE_FILE);
		BufferedReader urlTableReader = DiskFileHelperMethods.getNormalFileReader(urlTableFile);
		
		if(URLTABLE != null) return;
		URLTABLE = new ArrayList<URLTableEntry>();
		
		String urlTableLine;
		int sumOfLengths = 0;
		while((urlTableLine = urlTableReader.readLine()) != null) {
			String[] parts = urlTableLine.split(CommonsConstants.SPACE);
			
			String url = parts[1];
			int documentLength = Integer.parseInt(parts[2]);
			sumOfLengths += documentLength;
			
			URLTableEntry entry = new URLTableEntry(url, documentLength);
			URLTABLE.add(entry);
		}
		
		if(URLTABLE.size() != 0)
			AVG_DOC_LENGTH = sumOfLengths/URLTABLE.size();
		
		urlTableReader.close();
		System.out.println("Time to load URL Table = " + (System.currentTimeMillis() - millis) + "ms");
		
		
		CACHE = new Cache(Integer.parseInt(CONFIG.getProperty("cacheSize")),
				Integer.parseInt(CONFIG.getProperty("chunkSize")));
		
		
		millis = System.currentTimeMillis();
		
		BufferedReader fStream = new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG.getProperty("inputpath") +
				"/" + LEXICON_FILE)), 512000);
		
		VOCAB = new ArrayList<String>(); // Initialize VOCAB
		
		Splitter split = Splitter.on(CommonsConstants.SPACE);
		String dummyLine;
		while((dummyLine = fStream.readLine()) != null) {
			Iterator<String> splits = split.split(dummyLine).iterator();
			if(StringUtils.isAlpha(splits.next())) { VOCAB.add(dummyLine); } // just filtering alpha words
		}
		
		fStream.close();
		System.out.println("Time to load lexicon = " + (System.currentTimeMillis() - millis) + "ms");
	}

	private void checkForCorrectness(WordMetadata m) throws IOException {	
		InvertedListForWord list = InvertedListForWord.openList(m);
		
		System.out.println("Now checking nextGEQ");
		Posting p = InvertedListForWord.nextGEQ(list, new Posting(53136, 0));
		System.out.println("Doc ID = " + p.docId);
		System.out.println("Freq = " + p.freq);

		INVERTED_INDEX_FILE.seek(0);
	}
	
	private double computeBM25Component(int docId, int docFreq, int termFreq) {
		int totalDocuments = URLTABLE.size();
		int docLen = URLTABLE.get(docId).getDocumentLen();
		
		double K = 1.2*((1-0.75) + (0.75*(docLen/AVG_DOC_LENGTH)));
		double TFComp = ((1.2 + 1)*termFreq)/(K + termFreq);
		double IDFComp = Math.log((totalDocuments - docFreq + 0.5)/(docFreq + 0.5));
		
		return TFComp*IDFComp;
	}
	
	
	private void writeResults(Queue<DocumentScore> que) {
		int totalResults = Math.min(que.size(), TOTAL_REL_RESULTS);
		if(totalResults == 0) System.out.println("Sorry, couldnt find any documents for the search query. Try a new one.");
		
		for(int i=0; i<totalResults; i++) {
			DocumentScore dc = que.poll();
			System.out.print(dc.documentId + CommonsConstants.TAB + URLTABLE.get(dc.documentId-1).getURL() + 
					CommonsConstants.TAB);
			System.out.printf("%.2f", dc.bm25Score);
			System.out.print("(bm25Score)" + CommonsConstants.TAB);
			for(int j=0; j<dc.freqs.length; j++) {
				int index = j+1;
				System.out.print("(freqOf " + dc.words[j] + " =" + dc.freqs[j] + ")" + CommonsConstants.SPACE);
			}
			
			System.out.println();
		}
	}
	
	private void processConjunctiveQuery(List<WordMetadata> lists) throws IOException {
		// Open all lists. Means get all chunks of all words in memory. Check cache too for this.
		InvertedListForWord[] invertedLists = new InvertedListForWord[lists.size()];
		for(int i=0; i<lists.size(); i++) invertedLists[i] = InvertedListForWord.openList(lists.get(i));

		Posting did = new Posting(0, 0); // base posting
		List<Posting> tempList = new ArrayList<Posting>();
		Queue<DocumentScore> relevanceQueue = new PriorityQueue<DocumentScore>(Collections.reverseOrder());
		
		while(did.docId < MemoryBlock.MAX_DID.docId) {
			did = InvertedListForWord.nextGEQ(invertedLists[0], did);
			tempList.add(did);
			
			// see if we can find any intersection
			int largestDocId = did.docId;
			
			if(largestDocId == MemoryBlock.MAX_DID.docId) break; // reached end of this list
			
			for(int i=1; i<lists.size(); i++) {
				Posting d = InvertedListForWord.nextGEQ(invertedLists[i], did);
				if(d.docId == did.docId) tempList.add(d);
				else {
					largestDocId = d.docId;
					break;
				}
			}
			
			if(lists.size() == tempList.size()) { // found intersecting doc-id, we need to find bm25 score for this doc
				// Compute score
				double score = 0.0;
				int[] fres = new int[tempList.size()];
				String[] words = new String[tempList.size()];
				int docId = tempList.get(0).docId;
				for(int i=0; i<lists.size(); i++) {
					int docFreq = lists.get(i).totalPostings;
					int termFreq = tempList.get(i).freq;
					fres[i] = termFreq;
					words[i] = lists.get(i).word;
					score += computeBM25Component(docId, docFreq, termFreq);
				}
				
				relevanceQueue.add(new DocumentScore(docId, score, fres, words));
				did = new Posting(did.docId+1, 0);
			} else did = new Posting(largestDocId, 0);
			
			tempList.clear();
		}
		
		writeResults(relevanceQueue);
		for(int i=0; i<lists.size(); i++) InvertedListForWord.closeList(invertedLists[i]);
	}
	
	private void processDisjunctiveQuery(List<WordMetadata> lists) throws IOException {
		InvertedListForWord[] invertedLists = new InvertedListForWord[lists.size()];
		for(int i=0; i<lists.size(); i++) invertedLists[i] = InvertedListForWord.openList(lists.get(i));

		int n = lists.size();
		Map<Integer, int[]> mapOfPostings = new HashMap<Integer, int[]>();
		Queue<DocumentScore> relevanceQueue = new PriorityQueue<DocumentScore>(Collections.reverseOrder());
		
		for(int i=0; i<n; i++) {
			Posting did = new Posting(0, 0); // base posting
			while(did.docId < MemoryBlock.MAX_DID.docId) {
				Posting d = InvertedListForWord.nextGEQ(invertedLists[i], did);
				
				if(d.docId == MemoryBlock.MAX_DID.docId) break;
				
				int[] temp;
				if(mapOfPostings.containsKey(d.docId)) temp = mapOfPostings.get(d.docId);
				else temp = new int[n]; // we do not have any freq which is 0, so if for a doc, query term is not there, its slot=0
				
				temp[i] = d.freq;
				mapOfPostings.put(d.docId, temp);
				
				did = new Posting(d.docId+1, 0);
			}
		}
		
		// Compute Scores
		for(Integer docId : mapOfPostings.keySet()) {
			double score = 0.0;
			int[] fres = mapOfPostings.get(docId);
			String[] words = new String[n];
			for(int i=0; i<n; i++) {
				int docFreq = lists.get(i).totalPostings;
				int termFreq = fres[i];
				words[i] = lists.get(i).word;
				score += computeBM25Component(docId, docFreq, termFreq);
			}
			relevanceQueue.add(new DocumentScore(docId, score, fres, words));
		}
				
		writeResults(relevanceQueue);
		for(int i=0; i<lists.size(); i++) InvertedListForWord.closeList(invertedLists[i]);
	}
	
	private void processInput(String input, boolean isConjunctive) throws IOException {
		String[] queryParts = input.split(CommonsConstants.SPACE);
		
		Splitter splitter = Splitter.on(CommonsConstants.SPACE);
		List<WordMetadata> wordMetaDatas = new ArrayList<WordMetadata>();

		for(String queryPart : queryParts) {
			int index = Collections.binarySearch(VOCAB, queryPart, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.split(CommonsConstants.SPACE)[0].compareTo(o2);
				}
			});
			
			if(index < 0) {
				System.out.println("Query Term not found. Ignoring the whole query");
				break;
			} else {
				String meta = VOCAB.get(index);
				Iterator<String> splits = splitter.split(meta).iterator(); 
				//System.out.println(meta); 
				wordMetaDatas.add(new WordMetadata(splits.next(), Integer.parseInt(splits.next()), Integer.parseInt(splits.next()),
						Integer.parseInt(splits.next()), Integer.parseInt(splits.next()),
						Integer.parseInt(splits.next())));	
			}
		}
		
		// Process conjunctive/disjuntive queries
		Collections.sort(wordMetaDatas); // sort the lists from lowest to greatest number of postings
		
		if(isConjunctive) processConjunctiveQuery(wordMetaDatas); //checkForCorrectness(wordMetaDatas.get(0));
		else processDisjunctiveQuery(wordMetaDatas);
	}
	
	public void reset() throws IOException { INVERTED_INDEX_FILE.seek(0); }
	
	public static void analysis() throws IOException {
		String path = "C:/Users/Deepesh/Documents/Deepesh/nyu_classes/fall2016/web_search_engines/HW/QueryProcessor/output/";
		String file1 = path + "2007set";
		String file2 = path + "2008set";
		String file3 = path + "2009set";
		
		BufferedReader reader = DiskFileHelperMethods.getNormalFileReader(new File(file1));
		QueryAnalyser.run(reader);
		reader.close();
		
		reader = DiskFileHelperMethods.getNormalFileReader(new File(file2));
		QueryAnalyser.run(reader);
		reader.close();
		
		reader = DiskFileHelperMethods.getNormalFileReader(new File(file3));
		QueryAnalyser.run(reader);
		reader.close();
		
		long millis = System.currentTimeMillis();
		int totalWordsFound = 0;
		for(String word : QueryAnalyser.WORDS.keySet()) {
			int index = Collections.binarySearch(VOCAB, word, new Comparator<String>() {

				@Override
				public int compare(String arg0, String arg1) {
					return arg0.split(" ")[0].compareTo(arg1);
				}
			});
			
			if(index >=0) {
				totalWordsFound++;
			}
		}

		System.out.println("Time to find all words = " + (System.currentTimeMillis() - millis));
		System.out.println("Total words found = " + totalWordsFound + " out of " + QueryAnalyser.WORDS.size() );
	}
	
	public static void main(String[] args) {
		try {
			QueryProcessor qp = new QueryProcessor();
			
			// 1. Read the complete URLTable, Cache and Lexicon into Memory
			System.out.println("\n==================== Initializing the Query Processor ====================\n");
			long millis = System.currentTimeMillis();
			QueryProcessor.inititalizeQueryProcessor();
			System.out.println("\n======================= Query Processor Initialized ======================");
			
			
			
			// Ask for input until user provides it
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while(true) {
				System.out.print("\nPlease Enter Your Query here. Press ENTER if you want to exit: ");
				String input = br.readLine();
				if(input.equals("")) { System.out.println("\nExiting..Thanks!!\n"); qp.reset(); break; }
				
				System.out.print("Conjuntive(c)/Disjunctive(d): ");
				String conOrDis = br.readLine();
				System.out.println();
				boolean isConjunctive = conOrDis.equals("c") ? true : false;
				
				try {
					millis = System.currentTimeMillis();
					qp.processInput(input, isConjunctive);
					System.out.println("\n\nQuery Processed in " + (System.currentTimeMillis() - millis) + "ms");
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error Retrieving Results. Sorry!. Error is " + 
							e.getMessage() + ". Please try any other Query.");
				}
			}
			
			// TODO: 1. Create a static cache. That will be better. Poliy is LFU. For Dynamic cache LRU
			// 2. Disjunctive Queries
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "General Error, See stack trace below");
			e.printStackTrace();
		}
	}
}
