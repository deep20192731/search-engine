package websearch.intermediate;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.commoncrawl.examples.java_warc.IProcessWarcRecord;
import org.commoncrawl.examples.java_warc.SampleProcessWarcRecord;

import edu.cmu.lemurproject.WarcRecord;
import websearch.commons.CommonsConstants;
import websearch.commons.DiskFileHelperMethods;

public class IntermediatePostingsGenerator {
	private static final Properties CONFIG = getRawPostingsConfig();
	private static final Logger LOGGER = Logger.getLogger(IntermediatePostingsGenerator.class.getName());
	
	private static final String INTERMEDIATE_INDEX_NAME = "intermediateposting";
	private static final String INTERMEDIATE_URLTABLE_NAME = "urltable";
	
	private static final String WET_FILE_RECORD_NAME = "conversion";
	
	private static Properties getRawPostingsConfig() {
		Properties config = new Properties();
		try {
			config.load(IntermediatePostingsGenerator.class.
					getResourceAsStream(IntermediatePostingsConstants.CONFIG_FILENAME));
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, "Not able to open the config file " +
					IntermediatePostingsConstants.CONFIG_FILENAME);
		}
		return config;
	}
	
	private static long getGoodBlockSize(List<File> files) {
		long totalSizeInBytes = 0;
		/*for(File f : files) {
			totalSizeInBytes += f.length();
		}*/
		totalSizeInBytes = files.get(0).length();
		
		long blockSize = totalSizeInBytes/Integer.parseInt(CONFIG.getProperty("maxtempfiles"));
		long freeMem = Runtime.getRuntime().freeMemory();
		if(blockSize < freeMem/2) {
			//blockSize = freeMem/4;
		} else {
			LOGGER.log(Level.WARNING, "There might be memory issues, since block size is too much."
					+ " Try and reduce increase number of temp files");
		}
		System.out.println("Buffer size to use = " + blockSize + " bytes.");
		return blockSize;
	}
	
	public void generateIntermediatePostingsForWETFiles() throws FileNotFoundException, IOException {
		File baseFolder = new File(CONFIG.getProperty("inputpath"));
		List<File> files = Arrays.asList(baseFolder.listFiles());
		
		IProcessWarcRecord processor = new SampleProcessWarcRecord();
		
		int currentDocId = 1;
		int ignoredDocuments = 0;
		
		// Create buffer for output. This acts like infinite buffer
		BufferedFile indexFile = new BufferedFile(CONFIG.getProperty("outputpath") + "/" + 
							INTERMEDIATE_INDEX_NAME, 30000000, true);
		BufferedFile urlTableFile = new BufferedFile(CONFIG.getProperty("outputpath") + "/" + 
							INTERMEDIATE_URLTABLE_NAME, 30000000, false);
					
		for(int i=0; i<files.size(); i++) {
			File currentFile = files.get(i);
			DataInputStream inputStream = new DataInputStream(new 
					GZIPInputStream(new FileInputStream(currentFile)));
			
			HtmlParser parser = new HtmlParser();
			
			WarcRecord currentRecord;
			StringBuilder currentDocBuilder = new StringBuilder();
			
			long millis = System.currentTimeMillis();
				while((currentRecord = WarcRecord.readNextWarcRecord(inputStream)) != null) {
					//System.out.println("Time to read from WARC File = " + (System.currentTimeMillis()-millis));
					if(currentRecord.getHeaderRecordType().equals(WET_FILE_RECORD_NAME)) {
						// clear temp strutures
						if(currentDocBuilder.length() > 0) currentDocBuilder.setLength(0);
						
						String url = currentRecord.getHeaderMetadataItem("WARC-Target-URI");
						int contentLength = Integer.parseInt(currentRecord.getHeaderMetadataItem("Content-Length"));
							
						currentDocBuilder.append(currentRecord.getContentUTF8());
						//System.out.println("Time to read contents = " + (System.currentTimeMillis()-millis));
						List<String> wordsInCurrentDoc = parser.constructForWETFiles(currentDocBuilder, 
								currentDocId);
						/*Map<String, String> wordsInCurrentDoc = parser.constructForWETFilesV2(currentDocBuilder, 
								currentDocId);*/
							
						if(wordsInCurrentDoc.size() == 0) {
							ignoredDocuments++;
							LOGGER.log(Level.WARNING, "Ignoring Document because of no Valid-Words in " + 
									url);
							continue;
						}
						
						indexFile.addListToBuffer(wordsInCurrentDoc);
						urlTableFile.addToBuffer(currentDocId + CommonsConstants.SPACE + url + 
								CommonsConstants.SPACE + contentLength + "\n");
							
						currentDocId++;
					} else {
						ignoredDocuments++;
						LOGGER.log(Level.WARNING, "Ignoring since header didn't match. Header found = " + 
								currentRecord.getHeaderRecordType());
					}
				}
			
			System.out.println("Time taken for processing doc" + i + " = " + (System.currentTimeMillis()-millis));
		}
		indexFile.close();
		urlTableFile.close();
		
		System.out.println("Total docs processed = " + currentDocId);
		System.out.println("Total docs ignored = " + ignoredDocuments);
	}
	
	public void generateIntermediatePostings() throws IOException {
		// Get all Index/Data Files from the base folder
		File baseFolder = new File(CONFIG.getProperty("inputpath"));
		List<File> indexFiles = DiskFileHelperMethods.getFilesInFolder("index", baseFolder);
		List<File> dataFiles = DiskFileHelperMethods.getFilesInFolder("data", baseFolder);
		
		// HTML Parser used for parsing html pages
		HtmlParser parser = new HtmlParser();
		
		int currentDocId = 0;
		
		// Create buffer for output. This acts like infinite buffer
		BufferedFile indexFile = new BufferedFile(CONFIG.getProperty("outputpath") + "/" + 
				INTERMEDIATE_INDEX_NAME, getGoodBlockSize(dataFiles), true);
		BufferedFile urlTableFile = new BufferedFile(CONFIG.getProperty("outputpath") + "/" + 
				INTERMEDIATE_URLTABLE_NAME, getGoodBlockSize(indexFiles)*6, false);

		// Loop Over all Index/Data files and process one by one
		int totalDocsCounter = 0;
		for(int i=0; i<indexFiles.size(); i++) {
			long millis = System.currentTimeMillis();
			// Create Readers for reading html-pages
			BufferedReader indexFileReader = DiskFileHelperMethods.getGZIPReader(indexFiles.get(i));
			BufferedReader dataFileReader = DiskFileHelperMethods.getGZIPReader(dataFiles.get(i));

			String indexLine;
			while((indexLine = indexFileReader.readLine()) != null) {
				totalDocsCounter++;
				// Some pre-processing on the index line
				String[] indexParts = indexLine.split(" ");
				String url = indexParts[0];
				int bytesToRead = Integer.parseInt(indexParts[3]);
				
				if(!"ok".equals(indexParts[indexParts.length-1]))
					LOGGER.log(Level.WARNING, "Found 404. Ignoring");
				
				// Read from Data file. BytesRead = Found on Index file
				char[] buf = new char[bytesToRead];
				dataFileReader.read(buf);
				
				// Process the html page using the parser and get words-freq-docId triplets
				List<String> allWordsInDoc = new ArrayList<String>();
				try {
					allWordsInDoc = parser.parse(url, new String(buf), currentDocId);
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Ignoring document since parsing error");
					continue;
				}
				
				// write to buffer
				indexFile.addListToBuffer(allWordsInDoc);
				urlTableFile.addToBuffer(currentDocId + CommonsConstants.SPACE + url + 
						CommonsConstants.SPACE + bytesToRead + "\n");
				
				currentDocId++;
				
			}
			// Close all opened streams
			indexFileReader.close();
			dataFileReader.close();

			System.out.println("Time to process index-data file num" + i + " is " +
					(System.currentTimeMillis()-millis));
			
		}
		System.out.println("Total Docs Processed = " + totalDocsCounter);
		indexFile.close();
	}
	
	public static void main(String[] args) {
		long currentTimeInMillis = System.currentTimeMillis();
		try {
			IntermediatePostingsGenerator generator = new IntermediatePostingsGenerator();
			generator.generateIntermediatePostingsForWETFiles();
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "General Error. Stacktrace is below");
			e.printStackTrace();
		}

		System.out.println("Total time for Indexing in (ms) = " + 
				(System.currentTimeMillis()-currentTimeInMillis));
	}
}
