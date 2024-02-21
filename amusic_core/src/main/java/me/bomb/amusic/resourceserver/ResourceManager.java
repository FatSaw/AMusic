package me.bomb.amusic.resourceserver;

import java.io.File;
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
	private final boolean cache;
	
	public ResourceManager(int maxbuffersize, boolean cache) {
		this.maxbuffersize = maxbuffersize;
		this.cache = cache;
	}
	
	public UUID add(UUID targetplayer, File fileresource) {
		UUID token = UUID.randomUUID();
		CachedResource resource = null;
		Path path = fileresource.toPath();
		if (resources.containsKey(path)) {
			resource = resources.get(path);
		} else if (this.cache) {
			resource = new CachedResource(fileresource, maxbuffersize);
			resources.put(fileresource.toPath(), resource);
		} else {
			resource = new CachedResource(fileresource, maxbuffersize);
		}
		tokenres.put(token, resource);
		targets.put(targetplayer, token);
		return token;
	}

	public void setAccepted(UUID targetplayer) {
		if (!targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	protected boolean waitAcception(UUID token) {
		if(!targets.containsValue(token)) {
			return false;
		}
		return !accepted.contains(token);
	}

	protected byte[] get(UUID token) {
		if (token != null) {
			if (targets.containsValue(token)) {
				for (UUID target : targets.keySet()) {
					if (targets.get(target).equals(token)) {
						targets.remove(target);
						//PackApplyListener.reset(target);
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
	
	public void putResource(Path resourcepath, byte[] resource) {
		if(this.cache) {
			CachedResource cachedresource = new CachedResource(resource);
			resources.put(resourcepath, cachedresource);
		}
			
	}

	public void resetCache(Path resource) {
		if (resources.containsKey(resource)) {
			resources.remove(resource);
		}
	}

	public void remove(UUID targetuuid) {
		UUID token = targets.remove(targetuuid);
		if (token != null) {
			accepted.remove(token);
			tokenres.remove(token);
		}
	}

	public void clear() {
		resources.clear();
	}
	
	public boolean isCached(Path resource) {
		return resources.containsKey(resource);
	}
}
