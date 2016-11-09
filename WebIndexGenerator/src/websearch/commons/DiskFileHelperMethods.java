package websearch.commons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DiskFileHelperMethods {
	public static List<File> getFilesInFolder(String patternOfFileToRetrieve, File inputFolder) {
		File[] files = inputFolder.listFiles();
		
		List<File> allFiles = new ArrayList<File>();
		
		patternOfFileToRetrieve = patternOfFileToRetrieve == null ? "" : patternOfFileToRetrieve;

		for(File f: files) {
			if(f.getName().contains(patternOfFileToRetrieve)) {
				allFiles.add(f);
			}
		}
		return allFiles;
	}
	
	public static BufferedReader getGZIPReader(File file) throws IOException {
		BufferedReader reader = null;
		if(file == null)
			throw new IOException("File is null. So channel not created");

		GZIPInputStream streamForGZIP = new GZIPInputStream(new
				FileInputStream(file));
		InputStreamReader readerForGZIP = new InputStreamReader(streamForGZIP, CommonsConstants.CHARSET);
		reader = new BufferedReader(readerForGZIP);

		return reader;
	}
	
	public static BufferedReader getNormalFileReader(File file) throws IOException {
		BufferedReader reader = null;
		if(file == null)
			throw new IOException("File is null. So channel not created");

		FileInputStream stream = new FileInputStream(file);
		InputStreamReader readerForFile = new InputStreamReader(stream, CommonsConstants.CHARSET);
		reader = new BufferedReader(readerForFile, 1000000);

		return reader;
	}
	
}
