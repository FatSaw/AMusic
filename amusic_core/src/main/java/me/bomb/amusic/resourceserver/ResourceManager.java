package me.bomb.amusic.resourceserver;

import java.io.File;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class ResourceManager {
	
	private static MessageDigest md5hash;
	
	private final ConcurrentSkipListSet<UUID> accepted = new ConcurrentSkipListSet<UUID>();
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, CachedResource> tokenres = new ConcurrentHashMap<UUID, CachedResource>();
	private final ConcurrentHashMap<Path, CachedResource> resources = new ConcurrentHashMap<Path, CachedResource>();

	private final int maxbuffersize;
	private final boolean servercache, clientcache;
	private final byte[] salt;
	
	public ResourceManager(int maxbuffersize, boolean servercache, boolean clientcache, byte[] salt) throws NoSuchAlgorithmException {
		this.maxbuffersize = maxbuffersize;
		this.servercache = servercache;
		this.salt = salt;
		clientcache &= salt != null;
		if(clientcache) {
			md5hash = MessageDigest.getInstance("MD5");
		} else {
			md5hash = null;
		}
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
			resources.put(path, resource);
		} else {
			resource = new CachedResource(fileresource, maxbuffersize);
		}
		UUID token = null; //Token should be unique for each player.
		if(this.clientcache) { //Use salty token, to be hard predictable.
			int saltlength = salt.length, halfsalt = saltlength >> 1;
			--saltlength;
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
				md5hash.update(salt, 0, halfsalt);
				md5hash.update(resource.resource);
				md5hash.update(hash);
				md5hash.update(salt, halfsalt, saltlength);
				hash = md5hash.digest();
			}
			msb = (long) hash[0];
			msb |= (long) hash[0x01] << 0x08;
			msb |= (long) hash[0x02] << 0x10;
			msb |= (long) hash[0x03] << 0x18;
			msb |= (long) hash[0x04] << 0x20;
			msb |= (long) hash[0x05] << 0x28;
			msb |= (long) hash[0x06] << 0x30;
			msb |= (long) hash[0x07] << 0x38;
			lsb = (long) hash[0x08];
			lsb |= (long) hash[0x09] << 0x08;
			lsb |= (long) hash[0x0A] << 0x10;
			lsb |= (long) hash[0x0B] << 0x18;
			lsb |= (long) hash[0x0C] << 0x20;
			lsb |= (long) hash[0x0D] << 0x28;
			lsb |= (long) hash[0x0E] << 0x30;
			lsb |= (long) hash[0x0F] << 0x38;
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
