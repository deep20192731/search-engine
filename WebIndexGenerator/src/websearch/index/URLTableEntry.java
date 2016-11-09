package websearch.index;

public class URLTableEntry {
	private String url;
	private int documentLength;
	
	public URLTableEntry(String url, int len) {
		this.url = url;
		this.documentLength = len;
	}
	
	public String getURL() { return this.url; }
	public int getDocumentLen() { return this.documentLength; }
}
