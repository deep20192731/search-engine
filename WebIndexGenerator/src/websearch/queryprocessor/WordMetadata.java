package websearch.queryprocessor;

public class WordMetadata implements Comparable<WordMetadata> {
	public String word;
	public int chunkNum;
	public int blockInChunk;
	public int postingNumInChunk;
	public int totalPostings;
	public int chunkSpan;
	
	public WordMetadata(String w, int c, int b, int p, int t, int cS) {
		this.word = w;
		this.chunkNum = c; this.blockInChunk = b;
		this.postingNumInChunk = p; this.totalPostings = t;
		this.chunkSpan = cS;
	}

	@Override
	public int compareTo(WordMetadata o) {
		return this.totalPostings - o.totalPostings;
	}
}
