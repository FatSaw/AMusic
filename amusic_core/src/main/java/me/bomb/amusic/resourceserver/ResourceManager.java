package me.bomb.amusic.resourceserver;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class ResourceManager {
	
	private final ConcurrentSkipListSet<UUID> accepted = new ConcurrentSkipListSet<UUID>();
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, CachedResource> tokenres = new ConcurrentHashMap<UUID, CachedResource>();
	private final ConcurrentHashMap<Path, CachedResource> resources = new ConcurrentHashMap<Path, CachedResource>();

	private final int maxbuffersize;
	private final boolean servercache, clientcache;
	
	public ResourceManager(int maxbuffersize, boolean servercache, boolean clientcache) {
		this.maxbuffersize = maxbuffersize;
		this.servercache = servercache;
		this.clientcache = clientcache;
	}
	
	/**
	 * Loads fileresource into memory or takes it from cache (if enabled)
	 * Generates token
	 */
	public UUID add(UUID targetplayer, File fileresource) {
		CachedResource resource = null;
		Path path = fileresource.toPath();
		if (resources.containsKey(path)) {
			resource = resources.get(path);
		} else if (this.servercache) {
			resource = new CachedResource(fileresource, maxbuffersize);
			resources.put(fileresource.toPath(), resource);
		} else {
			resource = new CachedResource(fileresource, maxbuffersize);
		}
		UUID token = null;
		if(this.clientcache) {
			long msb = targetplayer.getMostSignificantBits(), lsb = targetplayer.getLeastSignificantBits();
			LongBuffer buffer = ByteBuffer.wrap(resource.hash).asLongBuffer();
			msb ^= buffer.get();
			lsb ^= buffer.get();
			token = new UUID(msb, lsb);
		} else {
			token = UUID.randomUUID();
		}
		tokenres.put(token, resource);
		targets.put(targetplayer, token);
		return token;
	}
	
	/**
	 * Set accept status by targetplayer uuid
	 */
	public void setAccepted(UUID targetplayer) {
		if (!targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	/**
	 * Checks needed wait resourcepack accept status by token
	 * @return true if token valid and no accept status.
	 */
	protected boolean waitAcception(UUID token) {
		if(!targets.containsValue(token)) {
			return false;
		}
		return !accepted.contains(token);
	}
	
	/**
	 * Get resource bytes by token
	 * Remove token
	 * Clears accept status
	 * @return 0 length byte array if token invalid
	 */
	protected byte[] get(UUID token) {
		if (token != null) {
			if (targets.containsValue(token)) {
				for (UUID target : targets.keySet()) {
					if (targets.get(target).equals(token)) {
						targets.remove(target);
					}
				}
			}
			accepted.remove(token);
			CachedResource cr = tokenres.remove(token);
			if (cr != null) {
				return cr.resource;
			}
		}
		return new byte[0];
	}

	/**
	 * Put resource into cache (if enabled)
	 */
	public void putResource(Path resourcepath, byte[] resource) {
		if(this.servercache) {
			CachedResource cachedresource = new CachedResource(resource);
			resources.put(resourcepath, cachedresource);
		}
			
	}

	/**
	 * Remove resource from cache
	 */
	public void resetCache(Path resource) {
		if (resources.containsKey(resource)) {
			resources.remove(resource);
		}
	}
	
	/**
	 * Remove token by player uuid
	 * Clears accept status
	 * @return true if removed
	 */
	public boolean remove(UUID targetuuid) {
		UUID token = targets.remove(targetuuid);
		if (token == null) {
			return false;
		}
		accepted.remove(token);
		tokenres.remove(token);
		return true;
	}
	
	/**
	 * Clear resource cache
	 */
	public void clear() {
		resources.clear();
	}
	
	/**
	 * Checks resource cached
	 */
	public boolean isCached(Path resource) {
		return resources.containsKey(resource);
	}
}
