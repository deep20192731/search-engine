package websearch.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import websearch.commons.DiskFileHelperMethods;
import websearch.intermediate.IntermediatePostingsConstants;

public class IndexCreator {
	private static final Properties CONFIG = getRawPostingsConfig();
	private static final Logger LOGGER = Logger.getLogger(IndexCreator.class.getName());
	
	private static final String POSTINGS_FILENAME = "finalmergedpostings";

	private static Properties getRawPostingsConfig() {
		Properties config = new Properties();
		try {
			config.load(IndexCreator.class.
					getResourceAsStream("index.properties"));
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, "Not able to open the config file " +
					IntermediatePostingsConstants.CONFIG_FILENAME);
		}
		return config;
	}


	public void createIndex() throws IOException{
		File fileToRead = new File(CONFIG.getProperty("inputpath") + POSTINGS_FILENAME);
		int chunkBufferRatio = Integer.parseInt(CONFIG.getProperty("chunkBufferRatio"));
		boolean writeAsBinary = Integer.parseInt(CONFIG.getProperty("writeasbinary")) == 0 ? false : true;
		
		BufferedReader mergedFileReader = DiskFileHelperMethods.getNormalFileReader(fileToRead);
		BufferredInvertedList buffer = new BufferredInvertedList(chunkBufferRatio, writeAsBinary,
				CONFIG.getProperty("outputpath"));
		
		String dummyLine;
		while((dummyLine = mergedFileReader.readLine()) != null) {
			String[] parts = dummyLine.split(" ");
			String word = parts[0];
			
			List<Integer> docIds = new ArrayList<Integer>();
			List<Integer> freqs = new ArrayList<Integer>();
			
			int c = -1;
			for(int i=1; i<parts.length; i+=2) {
				int docId = Integer.parseInt(parts[i]);
				// Some mistake in previous parts, since very small proportion is not coming as sorted, so we ignore those
				if(docId >= c) {
					docIds.add(docId);
					freqs.add(Integer.parseInt(parts[i+1]));
					c = docId;
				}
			}
			
			buffer.addToBuffer(word, docIds, freqs, 0); // this call will put all postings of this word
		}
		
		buffer.close();
		mergedFileReader.close();
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("=======Started to create index structures============");
			long millis = System.currentTimeMillis();
			IndexCreator indCreator = new IndexCreator();
			indCreator.createIndex();
			long executionTime = System.currentTimeMillis() - millis;
			System.out.println("=========Finished generating index structures in " + executionTime + "ms============");
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "General Error, See stack trace below");
			e.printStackTrace();
		}
	}
}
