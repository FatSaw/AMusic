package me.bomb.amusic.resourceserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import me.bomb.amusic.ConfigOptions;

public final class CachedResource {
	private static final ConcurrentSkipListSet<UUID> accepted = new ConcurrentSkipListSet<UUID>();
	private static final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private static final ConcurrentHashMap<UUID, CachedResource> tokenres = new ConcurrentHashMap<UUID, CachedResource>();
	private static final ConcurrentHashMap<Path, CachedResource> resources = new ConcurrentHashMap<Path, CachedResource>();
	private final byte[] resource;

	private CachedResource(File fileresource) {
		byte[] resource = new byte[ConfigOptions.maxpacksize];
		try {
			FileInputStream streamresource = new FileInputStream(fileresource);
			int size = streamresource.read(resource);
			streamresource.close();
			resource = Arrays.copyOf(resource, size);
		} catch (IOException e) {
		}
		this.resource = resource;
	}

	public static UUID add(UUID targetplayer, File fileresource) {
		UUID token = UUID.randomUUID();
		CachedResource resource = null;
		Path path = fileresource.toPath();
		if (resources.containsKey(path)) {
			resource = resources.get(path);
		} else if (ConfigOptions.cache) {
			resource = new CachedResource(fileresource);
			resources.put(fileresource.toPath(), resource);
		} else {
			resource = new CachedResource(fileresource);
		}
		tokenres.put(token, resource);
		targets.put(targetplayer, token);
		return token;
	}

	public static void setAccepted(UUID targetplayer) {
		if (!targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	protected static boolean waitAcception(UUID token) {
		if(!targets.containsValue(token)) {
			return false;
		}
		return !accepted.contains(token);
	}

	protected static byte[] get(UUID token) {
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

	public static void resetCache(Path resource) {
		if (resources.containsKey(resource)) {
			resources.remove(resource);
		}
	}

	public static void remove(UUID targetuuid) {
		UUID token = targets.remove(targetuuid);
		if (token != null) {
			accepted.remove(token);
			tokenres.remove(token);
		}
	}

	public static void clear() {
		resources.clear();
	}
	
	public static boolean isCached(Path resource) {
		return resources.containsKey(resource);
	}
}
