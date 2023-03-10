package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class CachedResource {
	private static final Map<UUID,UUID> targets = new HashMap<UUID,UUID>();
	private static final Map<UUID,CachedResource> tokenres = new HashMap<UUID,CachedResource>();
	private static final Map<Path,CachedResource> resources = new HashMap<Path,CachedResource>();
	private final byte[] resource;
	private CachedResource(File fileresource) {
		byte[] resource = new byte[50400000];
		try {
			FileInputStream streamresource = new FileInputStream(fileresource);
			int size = streamresource.read(resource);
			streamresource.close();
			resource = Arrays.copyOf(resource, size);
		} catch (IOException e) {
		}
		this.resource = resource;
		resources.put(fileresource.toPath(), this);
	}
	protected static UUID add(UUID targetplayer,File fileresource) {
		UUID token = UUID.randomUUID();
		if(!resources.containsKey(fileresource.toPath())) {
			new CachedResource(fileresource);
		}
		CachedResource res = resources.get(fileresource.toPath());
		tokenres.put(token, res);
		targets.put(targetplayer, token);
		return token;
	}
	protected static byte[] get(UUID token) {
		if(token!=null) {
			if(targets.containsValue(token)) {
				for(UUID target : targets.keySet()) {
					if(targets.get(target).equals(token)) {
						targets.remove(target);
					}
				}
			}
			if(tokenres.containsKey(token)) {
				return tokenres.remove(token).resource;
			}
		}
		return new byte[0];
	}
	protected static void resetCache(Path resource) {
		if(resources.containsKey(resource)) {
			resources.remove(resource);
		}
	}
	protected static void remove(UUID targetuuid) {
		UUID token = targets.remove(targetuuid);
		if(token!=null) {
			tokenres.remove(token);
		}
	}
	protected static void clear() {
		resources.clear();
	}
}
