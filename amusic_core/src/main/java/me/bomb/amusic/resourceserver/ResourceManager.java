package me.bomb.amusic.resourceserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class ResourceManager {
	
	
	private final ConcurrentSkipListSet<UUID> accepted;
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, byte[]> tokenres = new ConcurrentHashMap<UUID, byte[]>();
	private final ConcurrentHashMap<String, byte[]> resources;

	public final int maxbuffersize;
	private final byte[] salt;
	
	public ResourceManager(int maxbuffersize, boolean servercache, byte[] salt, boolean waitacception) {
		this.maxbuffersize = maxbuffersize;
		resources = servercache ? new ConcurrentHashMap<String, byte[]>() : null;
		this.salt = salt;
		accepted = waitacception ? new ConcurrentSkipListSet<UUID>() : null;
	}
	
	public byte[] readResource(File fileresource) {
		if(fileresource == null || !fileresource.isFile()) {
			return null;
		}
		long filesize = fileresource.length();
		if(filesize > maxbuffersize) {
			filesize = maxbuffersize;
		}
		byte[] resource = new byte[(int) filesize];
		try {
			FileInputStream streamresource = new FileInputStream(fileresource);
			final int size = streamresource.read(resource);
			streamresource.close();
			if(size < filesize) {
				resource = Arrays.copyOf(resource, size);
			}
		} catch (IOException e) {
			return null;
		}
		return resource;
	}
	
	/**
	 * Generate tokens
	 */
	public UUID[] generateTokens(byte[] resource, UUID... targetplayers) {
		int i = targetplayers.length;
		UUID[] tokens = new UUID[i];
		if(salt != null) {
			final MessageDigest md5hash;
			try {
				md5hash = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
			while(--i > -1) {
				UUID targetplayer = targetplayers[i];
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
				
				md5hash.reset();
				md5hash.update(resource);
				md5hash.update(hash);
				md5hash.update(this.salt);
				hash = md5hash.digest();
				
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
				final UUID token = new UUID(msb, lsb);
				tokens[i] = token;
				tokenres.put(token, resource);
				targets.put(targetplayer, token);
			}
			return tokens;
		}
		while(--i > -1) {
			final UUID token = UUID.randomUUID();
			tokens[i] = token;
			tokenres.put(token, resource);
			targets.put(targetplayers[i], token);
		}
		return tokens;
	}
	
	/**
	 * Set accept status by targetplayer uuid
	 */
	public void setAccepted(UUID targetplayer) {
		if (accepted == null || !targets.containsKey(targetplayer)) {
			return;
		}
		accepted.add(targets.get(targetplayer));
	}

	/**
	 * Checks needed wait resourcepack accept status by token
	 * @return true if token valid and no accept status.
	 */
	protected boolean waitAcception(UUID token) {
		if(accepted == null || !targets.containsValue(token)) {
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
		if(accepted != null) {
			accepted.remove(token);
		}
		return tokenres.remove(token);
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
		tokenres.remove(token);
		if(accepted == null) {
			return true;
		}
		accepted.remove(token);
		return true;
	}
	
	/**
	 * Get resource from cache
	 */
	public byte[] getCached(String id) {
		return resources == null ? null : resources.get(id);
	}
	
	/**
	 * Put resource into cache
	 */
	public void putCache(String id, byte[] resource) {
		if(resources == null) {
			return;
		}
		resources.put(id, resource);
			
	}

	/**
	 * Remove resource from cache
	 */
	public void removeCache(String id) {
		if(resources == null) {
			return;
		}
		resources.remove(id);
	}
	
	/**
	 * Clear resource cache
	 */
	public void clearCache() {
		if(resources == null) {
			return;
		}
		resources.clear();
	}
	
	/**
	 * Checks resource cached
	 */
	public boolean isCached(String id) {
		return resources != null && resources.containsKey(id);
	}
}
