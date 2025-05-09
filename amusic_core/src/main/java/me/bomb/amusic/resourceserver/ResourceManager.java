package me.bomb.amusic.resourceserver;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.net.ServerSocketFactory;

import me.bomb.amusic.http.ServerManager;

public final class ResourceManager {
	
	
	private final ConcurrentSkipListSet<UUID> accepted;
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, byte[]> tokenres = new ConcurrentHashMap<UUID, byte[]>();
	private final ConcurrentHashMap<String, byte[]> resources;

	public final int maxbuffersize;
	private final byte[] salt;
	private final ServerManager server;
	
	public ResourceManager(int maxbuffersize, boolean servercache, byte[] salt, boolean waitacception, final Collection<InetAddress> onlineips, final InetAddress ip, final int port, final int backlog, final int timeout, final ServerSocketFactory serverfactory, final short connectcount) {
		this.maxbuffersize = maxbuffersize;
		resources = servercache ? new ConcurrentHashMap<String, byte[]>() : null;
		this.salt = salt;
		accepted = waitacception ? new ConcurrentSkipListSet<UUID>() : null;
		this.server = new ServerManager(ip, port, backlog, timeout, serverfactory, onlineips, new ResourceSender(this), connectcount);
	}
	
	public void start() {
		server.start();
	}
	
	public void end() {
		server.end();
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
				
				lsb = hash[0x0F] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x0E] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x0D] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x0C] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x0B] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x0A] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x09] & 0xFF;
				lsb<<=8;
				lsb |= hash[0x08] & 0xFF;
				msb = hash[0x07] & 0xFF;
				msb<<=8;
				msb |= hash[0x06] & 0xFF;
				msb<<=8;
				msb |= hash[0x05] & 0xFF;
				msb<<=8;
				msb |= hash[0x04] & 0xFF;
				msb<<=8;
				msb |= hash[0x03] & 0xFF;
				msb<<=8;
				msb |= hash[0x02] & 0xFF;
				msb<<=8;
				msb |= hash[0x01] & 0xFF;
				msb<<=8;
				msb |= hash[0x00] & 0xFF;
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
