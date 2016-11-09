package websearch.queryprocessor;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import websearch.commons.CompressionDecompressionUtility;

public class MemoryBlock {
	public static Posting MAX_DID = new Posting(Integer.MAX_VALUE, 0);
	
	ByteBuffer buffer;
	
	public int docIdsSize;
	public int frqSize;
	
	public int indexForFirstPostingOfWord; // actual is index-1
	public int indexForLastPostingOfWord; // actual is index-1
	public int lastDocIdForBlock;
	
	public int rightPositionInBuffer;
	
	List<Integer> docIds;
	List<Integer> frqs;
	
	public static class Posting implements Comparable<Posting> {
		public int docId;
		public int freq;
		
		public Posting(int d, int f) { this.docId = d; this.freq = f; }

		@Override
		public int compareTo(Posting o) { return this.docId - o.docId; }
	}
	
	public MemoryBlock(byte[] blockContents, int firstDocIdIndex, int lastDocIdIndex, int lastDocId) {
		this.buffer = ByteBuffer.wrap(blockContents); // Position is automatically set where actual docIds start
		
		this.docIdsSize = this.buffer.getInt();
		this.frqSize = this.buffer.getInt();
		
		this.indexForFirstPostingOfWord = firstDocIdIndex;
		this.indexForLastPostingOfWord = lastDocIdIndex;
		this.lastDocIdForBlock = lastDocId;
		
		// Position is at the place where the actual docids begin(No compression here)
		this.rightPositionInBuffer = this.buffer.position();
	}	
	
	public void reset() { 
		this.buffer.position(this.rightPositionInBuffer);
		if(this.docIds != null) { this.docIds.clear(); this.docIds = null; }
		if(this.frqs != null) { this.frqs.clear(); this.frqs = null; }
	}
	
	public void get(byte[] a, int off) {
		int currentPosition = this.buffer.position();
		this.buffer.position(currentPosition + off);
		this.buffer.get(a);
		this.buffer.position(currentPosition); // resets this buffer's pointer back to position where block starts
	}
	
	public Posting nextGEQ(int did) {
		if(this.docIds == null || this.frqs == null) {
			// Decompress here
			byte[] docIdContents = new byte[this.docIdsSize];
			byte[] freqContents = new byte[this.frqSize];
			
			this.buffer.get(docIdContents);

			List<Integer> docIdsWithDiff = CompressionDecompressionUtility.
					decompressBytesWithVarByte(docIdContents);
			List<Integer> actualListForWord = docIdsWithDiff.subList(this.indexForFirstPostingOfWord,
					this.indexForLastPostingOfWord+1);

			this.docIds = CompressionDecompressionUtility.decompressWithDifferences(actualListForWord);
			
			this.buffer.get(freqContents);
			List<Integer> actualFreqsForWord = CompressionDecompressionUtility.
					decompressBytesWithVarByte(freqContents);
			this.frqs = actualFreqsForWord.subList(this.indexForFirstPostingOfWord,
					this.indexForLastPostingOfWord+1);

			assert(this.docIds.size() == this.frqs.size());
		}

		for(int i=0; i<this.docIds.size(); i++) {
			if(this.docIds.get(i) >= did)
				return new Posting(this.docIds.get(i), this.frqs.get(i));
		}
		
		return MAX_DID;
	}
	
}
