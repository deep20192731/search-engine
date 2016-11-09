package websearch.intermediate;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import websearch.commons.CommonsConstants;

public class BufferedFile {
	
	private List<String> traditionalBuffer;
	
	private long currentBufferSize;
	private long maxBufferSize;
	
	private Writer file;
	private String fileName;
	private int currentFileNumber;
	private boolean sortingAndMergingReq;
	
	private String getFileNumStrAndIncrement() {
		// Assuming temporary files are less than 10000. Doing since merge in Unix takes alphabetically
		String fileNum = "";
		if(this.currentFileNumber < 10)
			fileNum += "000" + this.currentFileNumber;
		else if(this.currentFileNumber >= 10 && this.currentFileNumber < 100)
			fileNum += "00" + this.currentFileNumber;
		else if(this.currentFileNumber >= 100 && this.currentFileNumber < 1000)
			fileNum += "0" + this.currentFileNumber;
		else
			fileNum += this.currentFileNumber;
		
		this.currentFileNumber++;
		return fileNum;
	}
	
	public BufferedFile(String filePath, long bufferSize, boolean sortingAndMergingReq) 
			throws UnsupportedEncodingException, FileNotFoundException {
		this.traditionalBuffer = new ArrayList<String>();
		this.currentBufferSize = 0;
		this.maxBufferSize = bufferSize;
		
		this.currentFileNumber = 0;
		this.fileName = filePath;

		createNewFile();
		this.sortingAndMergingReq = sortingAndMergingReq;
	}
	
	private void createNewFile() throws UnsupportedEncodingException, FileNotFoundException {
		this.file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName + 
				"_" + getFileNumStrAndIncrement()), CommonsConstants.CHARSET), 1024);
	}
	
	public long currentSize() {return this.currentBufferSize;}
	public long maxSize() {return this.maxBufferSize;}
	public long spaceAvailable() {return maxSize() - currentSize();}
	
	public void reset() throws IOException {
		this.traditionalBuffer.clear();
		this.currentBufferSize = 0;
		this.file.close();
		createNewFile();
	}
	
	public void close() throws IOException {
		writeToFile();
		this.file.close();
	}
	
	private int getLengthInBytes(String input) throws UnsupportedEncodingException {
		return input.getBytes("UTF-8").length;
	}
	
	private void writeToFile() throws IOException {
		int n = this.traditionalBuffer.size();
		if(this.sortingAndMergingReq) {
			Collections.sort(this.traditionalBuffer, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			});
			
			for(int i=0; i<n; i++) {
				String currentStr = this.traditionalBuffer.get(i).split(CommonsConstants.TAB)[0];
				StringBuilder currentStrBuilder = new StringBuilder(this.traditionalBuffer.get(i));
				
				int j = 1;
				String[] parts;
				while((i+j) < n && (parts = this.traditionalBuffer.get(i+j).split(CommonsConstants.TAB))[0].equals(currentStr)) {
					currentStrBuilder.append(CommonsConstants.TAB + parts[1]);
					j++;
				}
				currentStrBuilder.append("\n");
				this.file.append(currentStrBuilder.toString());
				i += j;
			}
		} else {
			for(String s : this.traditionalBuffer) {
				this.file.append(s);
			}
		}

		this.file.flush();
	}
	
	public void addToBuffer(String str) throws IOException {
		int strSize = getLengthInBytes(str);
		if(strSize <= spaceAvailable()) {
			this.traditionalBuffer.add(str);
			this.currentBufferSize += strSize;
		} else {
			writeToFile();
			reset();
			addToBuffer(str);
		}
	}
	
	public void addListToBuffer(List<String> list) throws IOException {
		for(String s : list) {
			addToBuffer(s);
		}
	}
}
