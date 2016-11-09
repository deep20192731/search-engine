package websearch.queryprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import websearch.queryprocessor.MemoryBlock.Posting;

public class InvertedListForWord {
	private static int TOTAL_POSTINGS_IN_BLOCK = 128;
	
	WordMetadata meta;
	List<MemoryBlock> blocks;

	public InvertedListForWord() { this.blocks = new ArrayList<MemoryBlock>(); }
	
	public static InvertedListForWord openList(WordMetadata queryTerm) throws IOException {
		QueryProcessor.INVERTED_INDEX_FILE.seek(0); // reset everytime(Very Important)
		
		InvertedListForWord invertedList = new InvertedListForWord();
		invertedList.meta = queryTerm;
		
		int chunkNumber = queryTerm.chunkNum;
		int totalChunksForThisWord = queryTerm.chunkSpan;
		
		List<MemoryChunk> memChunks = new ArrayList<MemoryChunk>();
		for(int i=chunkNumber; i<(chunkNumber+totalChunksForThisWord); i++) {
			if(QueryProcessor.CACHE.isCacheHit(i)) {
				memChunks.add(QueryProcessor.CACHE.getChunkFromCache(i));
			} else {
				long offsetInFile = (i-1)*QueryProcessor.CHUNK_SIZE;
				
				// every list sets this and reads once from disk. 
				// This is where the actual disk-access happen. Nowhere else
				if(offsetInFile < 0) {
					System.out.println(offsetInFile);
				}
				QueryProcessor.INVERTED_INDEX_FILE.seek(offsetInFile);
				byte[] chunkContents = new byte[(int) QueryProcessor.CHUNK_SIZE];
				QueryProcessor.INVERTED_INDEX_FILE.read(chunkContents);
				
				MemoryChunk mc = new MemoryChunk(chunkContents);
				memChunks.add(mc);
				
				// Add to Cache
				if(QueryProcessor.CACHE.isCacheFull()) QueryProcessor.CACHE.evictFromCache();
				QueryProcessor.CACHE.putChunkInCache(i, mc);
			}
		}
		
		// Now get the Blocks View for this Word-Postings
		int totalPostingsHovered = 0;
		boolean lastBlockReached = false;
		
		for(int i=0; i<memChunks.size(); i++) {
			MemoryChunk currentChunk = memChunks.get(i);
			int blockNum = (i == 0) ? queryTerm.blockInChunk : 1; // if first chunk then we know which block
			
			// since block num is one-indexed
			for(int j=blockNum-1; j<currentChunk.totalBlocks; j++) {
				// if firstBlock hovering, then we know the postings num
				int firstIndexInBlock = (totalPostingsHovered == 0) ? queryTerm.postingNumInChunk-1 : 0; // posting num too is one-indexed
				int totalPostsReadForWord = TOTAL_POSTINGS_IN_BLOCK - firstIndexInBlock;
				int remainingPostings = queryTerm.totalPostings - totalPostingsHovered;

				int lastIndexInBlock = TOTAL_POSTINGS_IN_BLOCK-1;
				
				if(remainingPostings <= totalPostsReadForWord) {
					lastBlockReached = true;
					totalPostsReadForWord = remainingPostings;
					lastIndexInBlock = Math.min(lastIndexInBlock, (firstIndexInBlock + remainingPostings - 1));
				}

				totalPostingsHovered += totalPostsReadForWord;
				
				// Add the block to word list. This block has some postings for the current word
				int sizeOfBlock = currentChunk.sizeOfBlocks[j];
				
				int offsetInChunk = 0;
				for(int z=0; z<j; z++) offsetInChunk += currentChunk.sizeOfBlocks[z];
					
				byte[] blockBytes = new byte[sizeOfBlock]; 
				currentChunk.get(blockBytes, offsetInChunk); // getting block bytes
				
				invertedList.blocks.add(new MemoryBlock(blockBytes, firstIndexInBlock,
						lastIndexInBlock, currentChunk.lastDocIds[j]));
				
				if(lastBlockReached) break; // means we have read all the postings
			}
		}
		return invertedList;
	}

	public static void closeList(InvertedListForWord list) throws IOException {
		list.blocks.clear();
		QueryProcessor.INVERTED_INDEX_FILE.seek(0);
	}
	
	public static Posting nextGEQ(InvertedListForWord invertedListForWord, Posting dummyPosting) {
		int did = dummyPosting.docId;
		
		// First figure out the right block index
		int blockIndex = -1;
		for(int i=0; i<invertedListForWord.blocks.size(); i++) {
			MemoryBlock mb = invertedListForWord.blocks.get(i);
			if(mb.lastDocIdForBlock >= did) {
				blockIndex = i;
				break;
			}
		}

		// this can be a problem with last doc, since lastDocId is of block, so if word posting
		// ends before that, then we need to uncompress the last block to see if we have anything
		// anyway we know the lastPostingIndex for the word
		if(blockIndex != -1) return invertedListForWord.blocks.get(blockIndex).nextGEQ(did); // this is sure to return positive
		else return invertedListForWord.blocks.get(invertedListForWord.blocks.size()-1).nextGEQ(did); // this may return MAX_DID
	}
}
