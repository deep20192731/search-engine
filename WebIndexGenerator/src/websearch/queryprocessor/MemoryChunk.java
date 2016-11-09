package websearch.queryprocessor;

import java.nio.ByteBuffer;

public class MemoryChunk {
	public ByteBuffer buffer;
	
	public int totalBlocks;
	public int[] lastDocIds;
	public int[] sizeOfBlocks;

	public int firstBlockStartPos; // in case we have to reset this position
	
	public MemoryChunk(byte[] contents) {
		this.buffer = ByteBuffer.wrap(contents);
		
		this.totalBlocks = this.buffer.getInt();
		this.lastDocIds = new int[totalBlocks];
		this.sizeOfBlocks = new int[totalBlocks];
		
		for(int i=0; i<totalBlocks; i++)
			this.lastDocIds[i] = this.buffer.getInt();
		
		for(int i=0; i<totalBlocks; i++)
			this.sizeOfBlocks[i] = this.buffer.getInt();	
		
		// After this the pointer is set to first block in the chunk
		this.firstBlockStartPos = this.buffer.position();
	}
	
	
	public void get(byte[] a, int off) {
		int currentPosition = this.buffer.position();
		this.buffer.position(currentPosition + off);
		this.buffer.get(a);
		this.buffer.position(currentPosition); // resets this buffer's pointer back to position where block starts
	}
	
	public int getTotalPostingsInChunk() { return this.totalBlocks*128; } // 128 postings in a block
	public int getTotalBlocks() { return this.totalBlocks; }
	public int[] getLastDocIds() { return this.lastDocIds; }
	public int[] getBlockSizes() { return this.sizeOfBlocks; }
	
	public void clear() { this.buffer.clear(); }
}
