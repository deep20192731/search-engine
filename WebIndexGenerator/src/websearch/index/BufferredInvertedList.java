package websearch.index;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import websearch.commons.CommonsConstants;
import websearch.commons.CompressionDecompressionUtility;

public class BufferredInvertedList {
	private static int TOTAL_CHUNKS = 0;
	private static String LEXICON_FILE_NAME = "lexicon";
	private static String INVERTED_INDEX_FILE_NAME = "inverted-index";
	
	private int totalChunksInBuffer;
	private boolean writeAsBinary;
	
	private List<Chunk> chunks;
	private List<LexiconEntry> wordEntries;
	
	private List<LexiconEntry> remainingEntries;
	
	DataOutputStream streamForInvertedIndex;
	DataOutputStream streamForLexicon;
	
	public BufferredInvertedList(int chunkBufferRatio, boolean writeAsBinary, String basePath) 
			throws FileNotFoundException {
		this.totalChunksInBuffer = chunkBufferRatio;
		this.writeAsBinary = writeAsBinary;
		
		this.chunks = new ArrayList<Chunk>();
		this.wordEntries = new ArrayList<LexiconEntry>();
		this.remainingEntries = new ArrayList<LexiconEntry>();
		
		this.streamForInvertedIndex = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(basePath + "/" + INVERTED_INDEX_FILE_NAME)));
		this.streamForLexicon = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(basePath + 
				"/" + LEXICON_FILE_NAME)));
	}
	
	private void writeAndReset() throws IOException {
		writeToFile();
		this.chunks.clear();
		this.wordEntries.clear();
		
		// After clearing word entries we fill it with others that are
		for(LexiconEntry e : this.remainingEntries)
			this.wordEntries.add(e);
		
		this.remainingEntries.clear();
	}

	private void writeToFile() throws IOException {
		writeAsBinary();
		this.streamForInvertedIndex.flush();
		this.streamForLexicon.flush();
	}

	
	private int addPaddingForChunkInIndex(Chunk chunk) throws IOException {
		if(!chunk.isFull()) {
			int rem = chunk.getRemaining();
			this.streamForInvertedIndex.write(new String(new char[rem]).getBytes());
			return rem;
		}
		return 0;
	}
	
	private void writeAsBinary() throws IOException {
		for(Chunk chunk : this.chunks) {
			List<Byte> allBytes = chunk.getBinaryContent();

			byte[] realBytes = new byte[allBytes.size()];
			for(int i=0; i<allBytes.size(); i++) realBytes[i] = allBytes.get(i);
			
			this.streamForInvertedIndex.write(realBytes);
			
			int padding = addPaddingForChunkInIndex(chunk);
		}
		
		for(LexiconEntry le : this.wordEntries) {
			int chunkNum = le.getBlocks().get(0).getChunkNum();
			int blockInChunk = le.getBlocks().get(0).getBlockNum();
			
			if(chunkNum == -1 || blockInChunk == -1) {// means word entry is there, but block yet not part of anything
				this.remainingEntries.add(le);
				System.out.println("Found the discrepancy case...");
				continue;
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(le.getWord());
			sb.append(CommonsConstants.SPACE);
			sb.append(chunkNum);
			sb.append(CommonsConstants.SPACE);
			sb.append(blockInChunk);
			sb.append(CommonsConstants.SPACE);
			sb.append(le.getPostingsNum());
			sb.append(CommonsConstants.SPACE);
			sb.append(le.getTotalPostings());
			sb.append(CommonsConstants.SPACE);
			sb.append(le.getChunkSpan());
			sb.append(CommonsConstants.NEW_LINE);
			
			this.streamForLexicon.writeBytes(sb.toString());
		}
		
		System.out.println("Written " + this.chunks.size() + " chunks and " +
				this.wordEntries.size() + " word-entries.");
	}

	/**
	 * Will write to file, if buffer size reached. Also, if a block is passed, it creates a new Chunk with 
	 * the passed block
	 * @param currentBlock
	 * @return
	 * @throws IOException 
	 */
	private Chunk createNewChunk() throws IOException {
		if(this.chunks.size() == this.totalChunksInBuffer)
			writeAndReset();

		Chunk c = new Chunk(++TOTAL_CHUNKS); // with global chunk number
		this.chunks.add(c);
		return c;
	}

	private Chunk getCurrentChunk() {
		if(this.chunks.size() > 0)
			return this.chunks.get(this.chunks.size()-1);
		return null;
	}
	
	public void addToBuffer(String word, List<Integer> docIds, List<Integer> freqs, int from) throws IOException {
		Chunk currentChunk = getCurrentChunk();
		if(currentChunk == null)
			currentChunk = createNewChunk();
		
		// We ensure that we send complete/partial empty block after the below para of code
		Block currentBlock = currentChunk.getPotentialBlockForThisChunk();
		if(currentBlock.isFull()) {
			boolean blockCanFit = currentChunk.testForCurrentBlockFit();
			if(!blockCanFit) {
				currentChunk = createNewChunk();
				currentChunk.addTempBlock(currentBlock);
			}
			
			currentChunk.addBlockToChunk(this.writeAsBinary);
			currentBlock = currentChunk.createNewTempBlockForChunk();	
		}
		
		// Create lexicon entry for this word
		LexiconEntry currentWord = new LexiconEntry(word, docIds.size());
		this.wordEntries.add(currentWord);
		
		// At this point we are sure we have an empty(complete/partial) chunk and block. 
		// Block though is not added to chunk. This might be possible that current chunk is not 
		// where this block gets added, if size of that block does not fit in chunk
		
		int numOfPostingsAdded = 0;
		int postingNumber = currentBlock.getCurrentPosition();
		currentWord.setPostingNum(postingNumber);
		
		while((numOfPostingsAdded = currentBlock.put(docIds, freqs, from)) != (docIds.size() - from)) {
			// Since we know atleast one posting will go in, we set the block for the word
			// When we print lexicons to file, we hope that its block is assigned to some chunk number
			currentWord.addBlock(currentBlock);
			
			if(currentBlock.isFull()) {
				boolean blockCanFit = currentChunk.testForCurrentBlockFit();
				if(!blockCanFit) {
					currentChunk = createNewChunk(); // if block cannot fit, we remove it from chunk. The temp block is added to new chunk
					currentChunk.addTempBlock(currentBlock);
				}
				
				// Only add when full
				// If, block is empty when word postings are over then the same block is used for next word
				// posting, since current chunk is mantained by this class, and current block is maintained by
				// that chunk
				currentChunk.addBlockToChunk(this.writeAsBinary); // add the temp block permanently to chunk list. Set the chunk num too
				currentBlock = currentChunk.createNewTempBlockForChunk();
			}
			from += numOfPostingsAdded;
		}
		
		currentWord.addBlock(currentBlock); // add the last block to the word
	}

	public void close() throws IOException {
		writeAndReset();
		System.out.println("Total Not Good BLocks = " + Block.DEBUG);
		this.streamForInvertedIndex.close();
		this.streamForLexicon.close();
	}
}
