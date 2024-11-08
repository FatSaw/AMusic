package me.bomb.amusic.resourceserver;

import java.io.File;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class ResourceManager {
	
	private final MessageDigest md5hash;
	
	private final ConcurrentSkipListSet<UUID> accepted = new ConcurrentSkipListSet<UUID>();
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, byte[]> tokenres = new ConcurrentHashMap<UUID, byte[]>();
	private final ConcurrentHashMap<Path, CachedResource> resources = new ConcurrentHashMap<Path, CachedResource>();

	public final int maxbuffersize;
	private final boolean servercache, clientcache, waitacception;
	private final byte[] salt;
	
	public ResourceManager(int maxbuffersize, boolean servercache, boolean clientcache, byte[] salt, boolean waitacception) {
		this.maxbuffersize = maxbuffersize;
		this.servercache = servercache;
		this.salt = salt;
		this.waitacception = waitacception;
		clientcache &= salt != null;
		MessageDigest md = null;
		if(clientcache) {
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				clientcache = false;
			}
		}
		this.md5hash = md;
		this.clientcache = clientcache;
	}
	
	/**
	 * Generate tokens
	 */
	public UUID[] generateTokens(byte[] resource, UUID... targetplayers) {
		int i = targetplayers.length;
		UUID[] tokens = new UUID[i];
		while(--i > -1) {
			UUID targetplayer = targetplayers[i];
			UUID token = null; //Token should be unique for each player.
			if(this.clientcache) { //Use salty token, to be hard predictable.
				long msb = targetplayer.getMostSignificantBits(), lsb = targetplayer.getLeastSignificantBits();
				byte[] hash = new byte[0x10];
				hash[0x00] = (byte) msb;
				msb>>=8;
				hash[0x01] = (byte) msb;
				msb>>=8;
				hash[0x02] = (byte) msb;
				msb>>=8;
				hash[0x03] = (byte) msb;
				msb>>=8;
				hash[0x04] = (byte) msb;
				msb>>=8;
				hash[0x05] = (byte) msb;
				msb>>=8;
				hash[0x06] = (byte) msb;
				msb>>=8;
				hash[0x07] = (byte) msb;
				hash[0x08] = (byte) lsb;
				lsb>>=8;
				hash[0x09] = (byte) lsb;
				lsb>>=8;
				hash[0x0A] = (byte) lsb;
				lsb>>=8;
				hash[0x0B] = (byte) lsb;
				lsb>>=8;
				hash[0x0C] = (byte) lsb;
				lsb>>=8;
				hash[0x0D] = (byte) lsb;
				lsb>>=8;
				hash[0x0E] = (byte) lsb;
				lsb>>=8;
				hash[0x0F] = (byte) lsb;
				synchronized (md5hash) {
					md5hash.reset();
					md5hash.update(resource);
					md5hash.update(hash);
					md5hash.update(this.salt);
					hash = md5hash.digest();
				}
				msb = hash[0x07];
				msb<<=8;
				msb += hash[0x06];
				msb<<=8;
				msb += hash[0x05];
				msb<<=8;
				msb += hash[0x04];
				msb<<=8;
				msb += hash[0x03];
				msb<<=8;
				msb += hash[0x02];
				msb<<=8;
				msb += hash[0x01];
				msb<<=8;
				msb += hash[0x00];
				lsb = hash[0x0F];
				lsb<<=8;
				lsb += hash[0x0E];
				lsb<<=8;
				lsb += hash[0x0D];
				lsb<<=8;
				lsb += hash[0x0C];
				lsb<<=8;
				lsb += hash[0x0B];
				lsb<<=8;
				lsb += hash[0x0A];
				lsb<<=8;
				lsb += hash[0x09];
				lsb<<=8;
				lsb += hash[0x08];
				token = new UUID(msb, lsb);
			} else {
				token = UUID.randomUUID();
			}
			tokenres.put(token, resource);
			targets.put(targetplayer, token);
			tokens[i] = token;
		}
		return tokens;
	}
	
	/**
	 * Loads fileresource into memory or takes it from cache (if enabled)
	 */
	public UUID[] add(File fileresource, UUID... targetplayers) {
		byte[] resource = null;
		Path path = fileresource.toPath();
		if (resources.containsKey(path)) {
			resource = resources.get(path).resource;
		} else if (this.servercache) {
			CachedResource cachedresource = new CachedResource(fileresource, maxbuffersize);
			resources.put(path, cachedresource);
			resource = cachedresource.resource;
		} else {
			resource = new CachedResource(fileresource, maxbuffersize).resource;
		}
		return this.generateTokens(resource, targetplayers);
	}
	
	/**
	 * Set accept status by targetplayer uuid
	 */
	public void setAccepted(UUID targetplayer) {
		if (!waitacception || !targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	/**
	 * Checks needed wait resourcepack accept status by token
	 * @return true if token valid and no accept status.
	 */
	protected boolean waitAcception(UUID token) {
		if(!waitacception || !targets.containsValue(token)) {
			return false;
		}
		return !accepted.contains(token);
	}
	
	/**
	 * Get resource bytes by token
	 * Remove token
	 * Clears accept status
	 * @return null if token invalid
	 */
	protected byte[] get(UUID token) {
		if (token == null) {
			return null;
		}
		if (targets.containsValue(token)) {
			for (UUID target : targets.keySet()) {
				if (targets.get(target).equals(token)) {
					targets.remove(target);
				}
			}
		}
		accepted.remove(token);
		return tokenres.remove(token);
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
