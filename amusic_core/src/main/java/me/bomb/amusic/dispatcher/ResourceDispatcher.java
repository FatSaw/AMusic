package me.bomb.amusic.dispatcher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.SoundInfo;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourceDispatcher {
	
	private final PackSender packsender;
	private final ResourceManager resourcemanager;
	private final PositionTracker positiontracker;
	private final byte[] host;
	private final int end;
	/**
	 * Dispatch resourcepack byte[] to targets
	 */
	public ResourceDispatcher(PackSender packsender, ResourceManager resourcemanager, PositionTracker positiontracker, String host) throws NullPointerException {
		if(packsender == null || resourcemanager == null || positiontracker == null || host == null) {
			throw new NullPointerException();
		}
		this.packsender = packsender;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.end = host.length();
		int i = this.end + 40;
		this.host = new byte[i];
		this.host[--i] = 'p';
		this.host[--i] = 'i';
		this.host[--i] = 'z';
		this.host[--i] = '.';
		i-=36;
		byte[] hostb = host.getBytes(StandardCharsets.UTF_8);
		while(--i > -1) {
			this.host[i] = hostb[i];
		}
	}
	
	/**
	 * Dispatch resourcepack byte[] to targets
	 */
	public final void dispatch(final String id, final UUID[] targets, final byte[] resource, final byte[] sha1, final SoundInfo[] sounds) {
		UUID[] tokens = resourcemanager.generateTokens(resource, targets);
		int i = tokens.length;
		byte[] host = new byte[this.host.length];
		System.arraycopy(this.host, 0, host, 0, this.host.length);
		while(--i > -1) {
			final UUID target = targets[i], token = tokens[i];
			final byte[] tokenbytes = token.toString().getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(tokenbytes, 0, host, this.end, 36);
			packsender.send(target, new String(host, StandardCharsets.UTF_8), sha1);
			positiontracker.setPlaylistInfo(target, id, sounds);
		}
	}

	/**
	 * Dispatch resourcepack file to targets
	 * File may be cached
	 */
	public final void dispatch(final String id, final UUID[] targets, final File resourcefile, final byte[] sha1, final SoundInfo[] sounds) {
		UUID[] tokens = resourcemanager.add(resourcefile, targets);
		int i = tokens.length;
		byte[] host = new byte[this.host.length];
		System.arraycopy(this.host, 0, host, 0, this.host.length);
		while(--i > -1) {
			final UUID target = targets[i], token = tokens[i];
			final byte[] tokenbytes = token.toString().getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(tokenbytes, 0, host, this.end, 36);
			packsender.send(target, new String(host, StandardCharsets.UTF_8), sha1);
			positiontracker.setPlaylistInfo(target, id, sounds);
		}
	}

}
