package websearch.queryprocessor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Cache {
	
	// Used to cache chunks that contain common query words
	private Map<Integer, MemoryChunk> cacheMap;
	private int maxChunksInCache;
	Queue<Integer> queue; // Simple FIFO Implementation as eviction policy
	
	public Cache(int maxCacheSize, int chunkSize) {
		this.cacheMap = new HashMap<Integer, MemoryChunk>();
		this.maxChunksInCache = maxCacheSize/chunkSize; // so cache size in properties should be of order of chunk-size
		this.queue = new LinkedList<Integer>();
	}
	
	public boolean isCacheFull() { return this.cacheMap.size() == maxChunksInCache; }
	public boolean isCacheHit(int key) { return this.cacheMap.containsKey(key); }
	public MemoryChunk getChunkFromCache(int key) { return this.cacheMap.get(key); }
	
	public MemoryChunk putChunkInCache(int key, MemoryChunk chunk) {
		this.queue.add(key);
		return this.cacheMap.put(key, chunk); 
	}
	
	public int evictFromCache() {
		int toRemove = this.queue.poll();
		this.cacheMap.remove(toRemove);
		return toRemove;
	}
	
	
}
