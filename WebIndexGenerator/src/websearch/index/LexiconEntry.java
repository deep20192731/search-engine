package websearch.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LexiconEntry implements Comparable<LexiconEntry> {
	private String word;
	private int chunkStartNum; // using int is justified since 20GB/256KB ~ 0.4million. Way less than 2147million, which is total nums repesented by ints
	private int blockNumInChunk;
	private int postingNumInBlock;
	private int totalPostings;
	private int chunksSpan;
	private List<Integer> chunksNumForThisWord; // number of chunks this inverted list span
	
	private List<Block> blocks;
	
	public LexiconEntry(String word, int totalPostings) {
		this.word = word;
		this.totalPostings = totalPostings;
		this.chunksNumForThisWord = new ArrayList<Integer>();
		this.blocks = new ArrayList<Block>();
	}
	
	public LexiconEntry(String word, int chunkNum, int blockNum, int postingNum, int totalPostings, int chunkSpan) {
		this.word = word;
		this.chunkStartNum = chunkNum;
		this.blockNumInChunk = blockNum;
		this.postingNumInBlock = postingNum;
		this.totalPostings = totalPostings;
		this.chunksSpan = chunkSpan;
		//this.chunksNumForThisWord = new ArrayList<Integer>();
		//this.blocks = new ArrayList<Block>();
	}
	
	public void setWord(String word) { this.word = word; }
	public void setChunkNum(int chunkNum) { this.chunkStartNum = chunkNum; }
	public void setBlockNum(int blockNum) { this.blockNumInChunk = blockNum; }
	public void setPostingNum(int postingNum) { this.postingNumInBlock = postingNum; }
	public void setTotalPostings(int total) { this.totalPostings = total; }
	public void setChunkSpan(int chunkNum) { this.chunksNumForThisWord.add(chunkNum); }
	public void addBlock(Block b) { this.blocks.add(b); }
	
	public int getChunkNum() { return this.chunkStartNum; }
	public int getBlockNum() { return this.blockNumInChunk; }
	public String getWord() { return this.word; }
	public List<Block> getBlocks() { return this.blocks; }
	public int getPostingsNum() { return this.postingNumInBlock + 1; } // +1 since 0 is not well encoded and in decompression we need to take care
	public int getTotalPostings() { return this.totalPostings; }
	
	public int getChunkSpan() {
		Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
		if(this.blocks.size() != 0) {
			for(Block b : this.blocks) {
				int chunkNum = b.getChunkNum();
				if(!map.containsKey(chunkNum))
					map.put(chunkNum, true);
			}
		}
		return map.size();
	}

	@Override
	public int compareTo(LexiconEntry o) {
		return this.word.compareTo(o.getWord());
	}
	
}
