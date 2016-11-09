package websearch.index;

import java.util.ArrayList;
import java.util.List;

import websearch.commons.CompressionDecompressionUtility;

public class Block {
	public static int TOTAL_POSTINGS = 128;

	public static int DEBUG = 0;
	private int chunkNum; // which chunkNum this block goes to
	private int blockNumInChunk; // block number in the chunk it goes
	
	private List<Byte> docsIds;
	private List<Byte> freqzs;
	
	private int currentPostingsInBlock;
	private int lastDocIdPosted;
	
	public Block() {
		this.docsIds = new ArrayList<Byte>();
		this.freqzs = new ArrayList<Byte>();
		
		this.chunkNum = -1; // means this block is still unassigned
		this.blockNumInChunk = -1;
		this.lastDocIdPosted = -1; // used when we take differences
		this.currentPostingsInBlock = 0;
	}
	
	public void setChunkNum(int c) { this.chunkNum = c; }
	public int getChunkNum() { return this.chunkNum; }
	
	public void setBlockNum(int c) { this.blockNumInChunk = c; }
	public int getBlockNum() { return this.blockNumInChunk; }
	
	public boolean isFull() { return (this.currentPostingsInBlock == TOTAL_POSTINGS );}
	
	public int getCurrentPosition() { return this.currentPostingsInBlock; }
	
	// Returns total postings put in this block
	public int put(List<Integer> docIds, List<Integer> freqs, int from) {
		int totalPostingsAdded = 0;
		if(!isFull()) {
			for(int i=this.currentPostingsInBlock; i<TOTAL_POSTINGS; i++) {
				int positionInActualPostings = from + (i-this.currentPostingsInBlock);
				if(positionInActualPostings < docIds.size()) { // if we have not put everything
					int docId = docIds.get(positionInActualPostings);
					int docIdToPut = (this.lastDocIdPosted == -1 || positionInActualPostings == 0)
							? docId : (docId - this.lastDocIdPosted); // last doc id for differencing
					this.lastDocIdPosted = docId;
					
					List<Byte> docIdBytes = CompressionDecompressionUtility.compressIntWithVarByte(docIdToPut);
					this.docsIds.addAll(docIdBytes);
					
					List<Byte> fres = CompressionDecompressionUtility.compressIntWithVarByte(
							freqs.get(positionInActualPostings));
					this.freqzs.addAll(fres);

					totalPostingsAdded += 1;
				} else break;
			}
			this.currentPostingsInBlock += totalPostingsAdded;
		}

		return totalPostingsAdded;
	}

	public int sizeInBytes() {
		int sizeInBytes = 0;
		sizeInBytes += this.docsIds.size() + this.freqzs.size(); // this is already in bytes
		sizeInBytes += 8; // since no compression for metadata
		
		return sizeInBytes;
	}


	public List<Byte> getBinaryContent() {
		List<Byte> bytes = new ArrayList<Byte>();

		// no compression for metadata
		bytes.addAll(CompressionDecompressionUtility.getBytesForInt(this.docsIds.size()));
		bytes.addAll(CompressionDecompressionUtility.getBytesForInt(this.freqzs.size()));
		
		bytes.addAll(this.docsIds);
		bytes.addAll(this.freqzs);
		
		return bytes;
	}
	
	public int getLastDocId() { return this.lastDocIdPosted; }
}
