package websearch.intermediate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;
import websearch.commons.CommonsConstants;
import edu.poly.cs912.Parser;

public class HtmlParser {
	private static final String SEP = " ";

	public List<Pair<String, Integer>> convertMapToPairsList(
			Map<String, Integer> wordsFreq) {
		List<Pair<String, Integer>> l = new ArrayList<Pair<String, Integer>>();
		for(String key : wordsFreq.keySet()) {
			l.add(Pair.with(key, wordsFreq.get(key)));
		}
		return l;
	}
	
	public Map<String, String> getListOfWordsAsMap(Map<String, Integer> wordsFreq, int docId) {
		Map<String, String> allWordsInDoc = new HashMap<String, String>();
		for(String key : wordsFreq.keySet()) {
			allWordsInDoc.put(key, docId + CommonsConstants.SPACE + wordsFreq.get(key));
		}
		return allWordsInDoc;
	}
	
	public List<String> getListOfWordsInDoc(Map<String, Integer> wordsFreq, int docId) {
		List<String> allWordsInDoc = new ArrayList<String>();
		for(String key : wordsFreq.keySet()) {
			allWordsInDoc.add(key + CommonsConstants.TAB + 
					docId + CommonsConstants.SPACE + wordsFreq.get(key));
		}
		return allWordsInDoc;
	}
	
	public boolean isStringWhitespace(String str) {
		return str.trim().length() == 0;
	}
	
	public List<Pair<String, Integer>> constructFreqWithDocIds(StringBuilder builder) {
		Map<String, Integer> wordsFreq = new HashMap<String, Integer>();
		String[] parsedStrParts = builder.toString().split("\n");
		for(String parts : parsedStrParts) {
			String word = parts.split(SEP)[0];
			/*if(isStringWhitespace(word))
				continue;*/
			
			if(wordsFreq.containsKey(word)) {
				wordsFreq.put(word, wordsFreq.get(word)+1);
			} else {
				wordsFreq.put(word, 1);
			}
		}
		
		return convertMapToPairsList(wordsFreq);
	}
	
	public Map<String, String> constructForWETFilesV2(StringBuilder builder, int docId) {
		Map<String, Integer> wordsFreq = new HashMap<String, Integer>();
		String[] parsedStrParts = builder.toString().split("\\s+");
		for(String word : parsedStrParts) {
			word = word.replaceAll("[^\\w]", "");
			// || !word.matches("[A-Za-z]+")
			if(isStringWhitespace(word))
				continue;
			
			word = word.toLowerCase();
			if(wordsFreq.containsKey(word)) {
				wordsFreq.put(word, wordsFreq.get(word)+1);
			} else {
				wordsFreq.put(word, 1);
			}
		}
		
		return getListOfWordsAsMap(wordsFreq, docId);
	}
	
	public List<String> constructForWETFiles(StringBuilder builder, int docId) {
		Map<String, Integer> wordsFreq = new HashMap<String, Integer>();
		String[] parsedStrParts = builder.toString().split("\\s+");
		for(String word : parsedStrParts) {
			word = word.replaceAll("[^\\w]", "");
			// || !word.matches("[A-Za-z]+")
			if(isStringWhitespace(word))
				continue;
			
			word = word.toLowerCase();
			if(wordsFreq.containsKey(word)) {
				wordsFreq.put(word, wordsFreq.get(word)+1);
			} else {
				wordsFreq.put(word, 1);
			}
		}
		
		return getListOfWordsInDoc(wordsFreq, docId);
	}
	
	public List<String> constructBuilder(StringBuilder builder, int docId) {
		Map<String, Integer> wordsFreq = new HashMap<String, Integer>();
		String[] parsedStrParts = builder.toString().split("\n");
		for(String parts : parsedStrParts) {
			String word = parts.split(SEP)[0];
			if(isStringWhitespace(word))
				continue;
			
			if(wordsFreq.containsKey(word)) {
				wordsFreq.put(word, wordsFreq.get(word)+1);
			} else {
				wordsFreq.put(word, 1);
			}
		}
		
		return getListOfWordsInDoc(wordsFreq, docId);
	}

	public List<String> parse(String url, String htmlContent, int docId) {
		// Returns(Word, Freq, DocId)
		StringBuilder builder = new StringBuilder();
		Parser.parseDoc(url, htmlContent, builder);
		return constructBuilder(builder, docId);
	}
	
	public List<Pair<String, Integer>> parseContents(String url, String htmlContent) {
		// Returns(Word, Freq, DocId)
		StringBuilder builder = new StringBuilder();
		Parser.parseDoc(url, htmlContent, builder);
		return constructFreqWithDocIds(builder);
	}
}
