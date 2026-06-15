package me.bomb.amusic.resourceserver;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.http.ServerManager;
import me.bomb.amusic.packedinfo.DataEntry;

public final class ResourceManager {
	
	private final ConcurrentSkipListSet<UUID> accepted;
	private final ConcurrentHashMap<UUID, UUID> targets = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, UUID> tokens = new ConcurrentHashMap<UUID, UUID>();
	private final ConcurrentHashMap<UUID, DataEntry> tokenres = new ConcurrentHashMap<UUID, DataEntry>();

	public final int maxbuffersize;
	private final byte[] salt;
	private final ServerManager server;
	
	private final PackSender packsender;
	private final PositionTracker positiontracker;
	private final byte[] host;
	private final int end;
	
	public ResourceManager(PackSender packsender, PositionTracker positiontracker, String host, int maxbuffersize, byte[] salt, boolean waitacception, final Collection<InetAddress> onlineips, final InetAddress ip, final int port, final int backlog, final int timeout, final ServerSocketFactory serverfactory, final short connectcount, Executor executorchecker, Executor executorsender) {
		if(packsender == null || positiontracker == null || host == null) {
			throw new NullPointerException();
		}
		byte[] hostb = host.getBytes(StandardCharsets.UTF_8);
		this.packsender = packsender;
		this.positiontracker = positiontracker;
		this.end = hostb.length;
		int i = this.end + 40;
		this.host = new byte[i];
		this.host[--i] = 'p';
		this.host[--i] = 'i';
		this.host[--i] = 'z';
		this.host[--i] = '.';
		i-=36;
		while(--i > -1) {
			this.host[i] = hostb[i];
		}
		
		this.maxbuffersize = maxbuffersize;
		this.salt = salt;
		accepted = waitacception ? new ConcurrentSkipListSet<UUID>() : null;
		this.server = new ServerManager(ip, port, backlog, timeout, serverfactory, onlineips, new ResourceSender(this, executorchecker, executorsender), connectcount);
	}
	
	

	public void start() {
		server.start();
	}
	
	public void end() {
		server.end();
	}
	
	/**
	 * Dispatch resourcepack file to targets
	 */
	public final boolean dispatch(final DataEntry dataentry, final UUID[] targets) {
		byte[] host = new byte[this.host.length];
		System.arraycopy(this.host, 0, host, 0, this.host.length);
		UUID target, token;
		int i = targets.length;
		if(salt == null) {
			while(--i > -1) {
				target = targets[i];
				token = UUID.randomUUID();
				UUID previoustoken = this.targets.put(target, token);
				if(previoustoken != null) {
					this.tokenres.remove(previoustoken);
					this.tokens.remove(previoustoken);
				}
				this.tokenres.put(token, dataentry);
				this.tokens.put(token, target);
				System.arraycopy(token.toString().getBytes(StandardCharsets.US_ASCII), 0, host, this.end, 36);
				positiontracker.stopMusic(target);
				positiontracker.removePlaylistInfo(target);
				packsender.send(target, new String(host, 0, host.length, StandardCharsets.UTF_8), dataentry.sha1);
				positiontracker.setPlaylistInfo(target, dataentry.name, dataentry.sounds);
			}
			return true;
		}
		final MessageDigest md5hash;
		try {
			md5hash = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		long msb, lsb;
		byte[] hash = new byte[0x10];
		while(--i > -1) {
			target = targets[i];
			msb = target.getMostSignificantBits();
			lsb = target.getLeastSignificantBits();
			hash[0x00] = (byte) msb;
			msb>>>=8;
			hash[0x01] = (byte) msb;
			msb>>>=8;
			hash[0x02] = (byte) msb;
			msb>>>=8;
			hash[0x03] = (byte) msb;
			msb>>>=8;
			hash[0x04] = (byte) msb;
			msb>>>=8;
			hash[0x05] = (byte) msb;
			msb>>>=8;
			hash[0x06] = (byte) msb;
			msb>>>=8;
			hash[0x07] = (byte) msb;
			hash[0x08] = (byte) lsb;
			lsb>>>=8;
			hash[0x09] = (byte) lsb;
			lsb>>>=8;
			hash[0x0A] = (byte) lsb;
			lsb>>>=8;
			hash[0x0B] = (byte) lsb;
			lsb>>>=8;
			hash[0x0C] = (byte) lsb;
			lsb>>>=8;
			hash[0x0D] = (byte) lsb;
			lsb>>>=8;
			hash[0x0E] = (byte) lsb;
			lsb>>>=8;
			hash[0x0F] = (byte) lsb;
			
			md5hash.reset();
			md5hash.update(dataentry.sha1);
			md5hash.update(hash);
			md5hash.update(this.salt);
			try {
				md5hash.digest(hash, 0x00, 0x10);
			} catch (DigestException e) {
				continue;
			}
			
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
			token = new UUID(msb, lsb);
			this.targets.put(target, token);
			this.tokenres.put(token, dataentry);
			this.tokens.put(token, target);
			System.arraycopy(token.toString().getBytes(StandardCharsets.US_ASCII), 0, host, this.end, 36);
			positiontracker.stopMusic(target);
			positiontracker.removePlaylistInfo(target);
			packsender.send(target, new String(host, 0, host.length, StandardCharsets.UTF_8), dataentry.sha1);
			positiontracker.setPlaylistInfo(target, dataentry.name, dataentry.sounds);
		}
		return true;
	}
	
	/**
	 * Set accept status by targetplayer uuid
	 */
	public void setAccepted(UUID target) {
		UUID token;
		if (accepted == null || (token = targets.get(target)) == null) {
			return;
		}
		accepted.add(token);
	}

	/**
	 * Checks needed wait resourcepack accept status by token
	 * @return true if token valid and no accept status.
	 */
	protected boolean waitAcception(UUID token) {
		if(accepted == null || !tokens.containsKey(token)) {
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
	protected DataEntry get(UUID token) {
		UUID target;
		if (token == null || (target = tokens.get(token)) == null || !token.equals(targets.get(target))) {
			return null;
		}
		return tokenres.get(token);
		
		/*UUID target;
		if (token == null || (target = tokens.remove(token)) == null || !token.equals(targets.remove(target))) {
			return null;
		}
		if(accepted != null) {
			accepted.remove(token);
		}
		return tokenres.remove(token);*/
	}
	
	/**
	 * Remove token by player uuid
	 * Clears accept status
	 * @return true if removed
	 */
	public boolean remove(UUID target) {
		UUID token;
		if (target == null || (token = targets.remove(target)) == null) {
			return false;
		}
		tokens.remove(token);
		tokenres.remove(token);
		if(accepted == null) {
			return true;
		}
		accepted.remove(token);
		return true;
	}
}
