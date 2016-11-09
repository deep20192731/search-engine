package websearch.queryprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import websearch.commons.DiskFileHelperMethods;

public class QueryAnalyser {
	
	
	public static Map<String, Integer> WORDS = new HashMap<String, Integer>();
	
	public static void run(BufferedReader reader) throws IOException {
		String dummyLine;
		while((dummyLine = reader.readLine()) != null) {
			String[] parts = dummyLine.split(":");
			String[] words = parts[1].split(" ");
			
			for(String word : words) {
				if(WORDS.containsKey(word)) {
					WORDS.put(word, WORDS.get(word)+1);
				} else {
					WORDS.put(word, 1);
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		long millis = System.currentTimeMillis();
		
		String path = "C:/Users/Deepesh/Documents/Deepesh/nyu_classes/fall2016/web_search_engines/HW/QueryProcessor/output/";
		String file1 = path + "2007set";
		String file2 = path + "2008set";
		String file3 = path + "2009set";
		
		Map<String, Integer> freqMap = new HashMap<String, Integer>();
		
		BufferedReader reader = DiskFileHelperMethods.getNormalFileReader(new File(file1));
		run(reader);
		reader.close();
		
		reader = DiskFileHelperMethods.getNormalFileReader(new File(file2));
		run(reader);
		reader.close();
		
		reader = DiskFileHelperMethods.getNormalFileReader(new File(file3));
		run(reader);
		reader.close();
		
		
		System.out.println("Total Unique Words = " + WORDS.size());
		
	}
}
