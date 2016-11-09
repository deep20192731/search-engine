package websearch.index;

import java.util.ArrayList;
import java.util.List;

import websearch.commons.CompressionDecompressionUtility;

public class Chunk {
	public static int CHUNK_SIZE = 256000;
	
	private int globalChunkNum;
	private List<Block> blocks; // blocks do not overlap a chunk
	
	private Block tempBlock; // temp block! When this is full, chunk-block-fit test should run and decide to add this into permanent list
	private int currentSizeInBytes;

	public Chunk(int chunkNum) {
		this.globalChunkNum = chunkNum;
		this.blocks = new ArrayList<Block>();
		this.currentSizeInBytes = 4; // includes size of int(total-blocks)
	}
	
	public int getLastBlockNum() { return this.blocks.size(); }
	
	public boolean isFull() {
		if(this.currentSizeInBytes == CHUNK_SIZE)
			return true;
		return false;
	}
	
	public int getRemaining() {
		return CHUNK_SIZE - currentSizeInBytes;
	}
	
	public int totalBlocks() { return this.blocks.size(); }

	// If no current block then, it creats new
	public Block getPotentialBlockForThisChunk() { 
		if(this.tempBlock == null)
			this.tempBlock = new Block();
		return this.tempBlock; 
	}

	public boolean testForCurrentBlockFit() {
		Block currentBlock = this.tempBlock;
		if(currentBlock == null)
			return false;
		
		int extraSizeInBytes = currentBlock.sizeInBytes(); // actual size of block;
		extraSizeInBytes += 4; // size of int(lastDocId) after introduction
		extraSizeInBytes += 4; // size of int(blockSize) after introduction
		extraSizeInBytes += 0; // size of int(totalBlocks) after introduction of new block (already included)
		
		if(extraSizeInBytes <= (CHUNK_SIZE - this.currentSizeInBytes))
			return true;
		
		return false;
	}

	
	public Block createNewTempBlockForChunk() {
		this.tempBlock = new Block();
		return this.tempBlock;
	}

	public void addTempBlock(Block b) { this.tempBlock = b; }
	
	public void addBlockToChunk(boolean asBinary) {
		// Check for the block fit is already done in Buffer
		this.blocks.add(this.tempBlock);
		//this.lastDocsIdsForBlocks.add(this.tempBlock.getLastDocId());
		
		this.tempBlock.setChunkNum(this.globalChunkNum); // set the chunkNum for this block
		this.tempBlock.setBlockNum(this.blocks.size()); // set the block num in current chunk
		
		// Update sizes
		this.currentSizeInBytes += this.tempBlock.sizeInBytes();
		this.currentSizeInBytes += 4;
		this.currentSizeInBytes += 4;
		
		this.tempBlock = null;
	}
	
	public List<Byte> getBinaryContent() {
		List<Byte> bytes = new ArrayList<Byte>();
		bytes.addAll(CompressionDecompressionUtility.getBytesForInt(this.blocks.size()));
		
		for(Block b : this.blocks)
			bytes.addAll(CompressionDecompressionUtility.getBytesForInt(b.getLastDocId()));
		
		for(Block b : this.blocks)
			bytes.addAll(CompressionDecompressionUtility.getBytesForInt(b.sizeInBytes()));
		
		for(Block b : this.blocks)
			bytes.addAll(b.getBinaryContent());
		
		for(Block b : this.blocks) {
			if(!b.isFull())
				System.out.println("Not Good...");
		}
		return bytes;
	}
}
