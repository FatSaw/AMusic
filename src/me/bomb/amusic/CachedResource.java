package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CachedResource {
	private static final Set<UUID> accepted = new HashSet<UUID>();
	private static final Map<UUID, UUID> targets = new HashMap<UUID, UUID>();
	private static final Map<UUID, CachedResource> tokenres = new HashMap<UUID, CachedResource>();
	private static final Map<Path, CachedResource> resources = new HashMap<Path, CachedResource>();
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

	protected static UUID add(UUID targetplayer, File fileresource) {
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

	protected static void setAccepted(UUID targetplayer) {
		if (!targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	protected static boolean waitAcception(UUID token) {
		return token == null && targets.containsValue(token) ? false : !accepted.contains(token);
	}

	protected static byte[] get(UUID token) {
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

	protected static void resetCache(Path resource) {
		if (resources.containsKey(resource)) {
			resources.remove(resource);
		}
	}

	protected static void remove(UUID targetuuid) {
		UUID token = targets.remove(targetuuid);
		if (token != null) {
			accepted.remove(token);
			tokenres.remove(token);
		}
	}

	protected static void clear() {
		resources.clear();
	}
}
