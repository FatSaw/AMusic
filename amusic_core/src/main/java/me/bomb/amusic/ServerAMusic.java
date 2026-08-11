package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;

import me.bomb.amusic.packedinfo.CustomDatastore;
import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.packedinfo.ResourcepackInfo;
import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.packedinfo.SoundSource;
import me.bomb.amusic.packedinfo.SourceEntry;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.util.ByteArraysOutputStream;
import me.bomb.amusic.util.Logger;

public final class ServerAMusic extends LocalAMusic implements Runnable {
	private final InetAddress hostip, remoteip;
	private final int port, timeout, backlog;
	private final ServerSocketFactory connectserverfactory;
	private volatile boolean run;
	private ServerSocket server;
	private final Executor serverexecutor;
	
	public ServerAMusic(Logger logger, Executor executor, SoundSource<? extends SourceEntry> soundsource, PositionTracker positiontracker, ResourceManager resourcemanager, Data datamanager, InetAddress hostip, InetAddress remoteip, int port, int backlog, ServerSocketFactory connectserverfactory, Executor serverexecutor) {
		super(logger, executor, soundsource, positiontracker, resourcemanager, datamanager);
		this.hostip = hostip;
		this.remoteip = remoteip;
		this.port = port;
		this.timeout = 5000;
		this.backlog = backlog;
		this.connectserverfactory = connectserverfactory;
		this.serverexecutor = serverexecutor;
	}

	@Override
	public void enable() {
		super.enable();
		this.run = true;
		new Thread(this).start();
	}

	@Override
	public void disable() {
		super.disable();
		run = false;
		if(server==null) return;
		try {
			server.close();
		} catch (IOException e) {
		}
	}
	
	public final byte[] getPlayersLoadedBytes(byte[] playlistnameb) {
		if(playlistnameb.length > 0xFF || playlistnameb.length < 0x01) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, 0, playlistnameb.length, StandardCharsets.UTF_8);
		UUID[] playeruuids = positiontracker.getPlayersLoaded(playlistname);
		if(playeruuids == null) {
			playeruuids = new UUID[0];
		}
		int i = playeruuids.length, j = 3;
		byte[] response = new byte[4 + (i<<4)];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		response[2] = (byte) (i>>>16);
		response[3] = (byte) (i>>>24);
		while(--i > -1) {
			UUID playeruuid = playeruuids[i];
			long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			msb>>>=8;
			response[++j] = (byte) msb;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
			lsb>>>=8;
			response[++j] = (byte) lsb;
		}
		return response;
	}
	
	public final byte[] getPlaylistsBytes(byte[] packedb) {
		if(packedb.length != 1) {
			return new byte[0];
		}
		String[] playlists = packedb[0] == 1 ? datamanager.listResourcepacks() : soundsource.listResourcepacks();
		int playlistcount = playlists.length;
		if(playlistcount > 65535) {
			playlistcount = 65535;
		}
		int i = playlistcount;
		int totallengths = 0;
		byte[] lengths = new byte[i];
		byte[][] anames = new byte[i][];
		while(--i > -1) {
			byte[] namebytes = playlists[i].getBytes(StandardCharsets.UTF_8);
			int length = namebytes.length;
			if(length > 0xFF) {
				length = 0xFF;
				byte[] nnamebytes = new byte[0xFF];
				System.arraycopy(namebytes, 0, nnamebytes, 0, length);
				namebytes = nnamebytes;
			}
			lengths[i] = (byte) length;
			anames[i] = namebytes;
			totallengths += length;
		}
		i = playlistcount;
		byte[] response = new byte[2 + i + totallengths];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		System.arraycopy(lengths, 0, response, 2, i);
		int pos = 2 + i;
		while(--i > -1) {
			byte[] name = anames[i];
			int length = name.length;
			System.arraycopy(name, 0, response, pos, length);
			pos+=length;
		}
		return response;
	}
	
	public final byte[] getPlaylistSoundnamesPlaylistnameBytes(byte[] playlistnamepackedb) {
		if(playlistnamepackedb.length > 256 || playlistnamepackedb.length < 1) {
			return new byte[0];
		}
		boolean packed = playlistnamepackedb[0] == 1;
		String playlistname = new String(playlistnamepackedb, 1, playlistnamepackedb.length - 1, StandardCharsets.UTF_8);
		if(packed) {
			SoundInfo[] soundinfos;
			ResourcepackInfo info;
			DataEntry dataentry;
			if((dataentry = datamanager.getResourcepack(playlistname)) == null || (info = dataentry.info) == null || (soundinfos = info.getSounds()) == null) {
				return new byte[0];
			}
			int soundcount = soundinfos.length;
			if(soundcount > 65535) {
				soundcount = 65535;
			}
			int i = soundcount;
			int totallengths = 0;
			byte[] lengths = new byte[i];
			byte[][] anames = new byte[i][];
			while(--i > -1) {
				byte[] namebytes = soundinfos[i].name.getBytes(StandardCharsets.UTF_8);
				int length = namebytes.length;
				if(length > 0xFF) {
					length = 0xFF;
					byte[] nnamebytes = new byte[0xFF];
					System.arraycopy(namebytes, 0, nnamebytes, 0, length);
					namebytes = nnamebytes;
				}
				totallengths += length;
				anames[i] = namebytes;
				lengths[i] = (byte) length;
			}
			i = soundcount;
			byte[] response = new byte[2 + i + totallengths];
			response[0] = (byte)i;
			response[1] = (byte) (i>>>8);
			System.arraycopy(lengths, 0, response, 2, i);
			int j = 2 + i;
			while(--i > -1) {
				byte[] name = anames[i];
				int length = name.length;
				System.arraycopy(name, 0, response, j, length);
				j+=length;
			}
			return response;
		} else {
			//
			String[] sounds = soundsource.getSounds(playlistname);
			if(sounds==null) {
				return new byte[0];
			}
			int soundcount = sounds.length;
			if(soundcount > 65535) {
				soundcount = 65535;
			}
			int i = soundcount;
			int totallengths = 0;
			byte[] lengths = new byte[i];
			byte[][] anames = new byte[i][];
			while(--i > -1) {
				byte[] namebytes = sounds[i].getBytes(StandardCharsets.UTF_8);
				int length = namebytes.length;
				if(length > 0xFF) {
					length = 0xFF;
					byte[] nnamebytes = new byte[0xFF];
					System.arraycopy(namebytes, 0, nnamebytes, 0, length);
					namebytes = nnamebytes;
				}
				totallengths += length;
				anames[i] = namebytes;
				lengths[i] = (byte) length;
			}
			i = soundcount;
			byte[] response = new byte[2 + i + totallengths];
			response[0] = (byte)i;
			response[1] = (byte) (i>>>8);
			System.arraycopy(lengths, 0, response, 2, i);
			int j = 2 + i;
			while(--i > -1) {
				byte[] name = anames[i];
				int length = name.length;
				System.arraycopy(name, 0, response, j, length);
				j+=length;
			}
			return response;
		}
	}
	
	public final byte[] getPlaylistSoundnamesPlayeruuidBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = positiontracker.getResourcepackInfo(playeruuid)) == null || (soundsinfo = info.getSounds()) == null) {
			return new byte[0];
		}
		int soundcount = soundsinfo.length;
		if(soundcount > 65535) {
			soundcount = 65535;
		}
		int i = soundcount;
		int totallengths = 0;
		byte[] lengths = new byte[i];
		byte[][] anames = new byte[i][];
		while(--i > -1) {
			byte[] namebytes = soundsinfo[i].name.getBytes(StandardCharsets.UTF_8);
			int length = namebytes.length;
			if(length > 0xFF) {
				length = 0xFF;
				byte[] nnamebytes = new byte[0xFF];
				System.arraycopy(namebytes, 0, nnamebytes, 0, length);
				namebytes = nnamebytes;
			}
			lengths[i] = (byte) length;
			anames[i] = namebytes;
			totallengths += length;
		}
		i = soundcount;
		byte[] response = new byte[2 + i + totallengths];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		System.arraycopy(lengths, 0, response, 2, i);
		int pos = 2 + i;
		while(--i > -1) {
			byte[] name = anames[i];
			int length = name.length;
			System.arraycopy(name, 0, response, pos, length);
			pos+=length;
		}
		return response;
	}
	
	public final byte[] getPlaylistSoundlengthsPlaylistnameBytes(byte[] playlistnameb) {
		if(playlistnameb.length > 255 || playlistnameb.length < 1) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, StandardCharsets.UTF_8);
		SoundInfo[] soundinfos;
		ResourcepackInfo info;
		DataEntry dataentry;
		if((dataentry = datamanager.getResourcepack(playlistname)) == null || (info = dataentry.info) == null || (soundinfos = info.getSounds()) == null) {
			return new byte[0];
		}
		int soundcount = soundinfos.length;
		if(soundcount > 65535) {
			soundcount = 65535;
		}
		int i = soundcount, j = 2 + (i << 1);
		byte[] response = new byte[j];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		while(--i > -1) {
			short length = soundinfos[i].length;
			response[--j] = (byte) length;
			length>>>=8;
			response[--j] = (byte) length;
		}
		return response;
	}
	
	public final byte[] getPlaylistSoundlengthsPlayeruuidBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		ResourcepackInfo info;
		SoundInfo[] soundsinfo;
		if ((info = positiontracker.getResourcepackInfo(playeruuid)) == null || (soundsinfo = info.getSounds()) == null) {
			return new byte[0];
		}
		int soundcount = soundsinfo.length;
		if(soundcount > 65535) {
			soundcount = 65535;
		}
		int i = soundcount, j = 2 + (i << 1);
		byte[] response = new byte[j];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		while(--i > -1) {
			short length = soundsinfo[i].length;
			response[--j] = (byte) length;
			length>>>=8;
			response[--j] = (byte) length;
		}
		return response;
	}
	
	public final void setRepeatModeBytes(byte[] playeruuidrepeatb) {
		if(playeruuidrepeatb.length != 17) {
			return;
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidrepeatb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidrepeatb[0x08] & 0xFF;
		msb = playeruuidrepeatb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidrepeatb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		byte repeat = playeruuidrepeatb[0x10];
		positiontracker.setRepeater(playeruuid, repeat == 1 ? RepeatType.PLAYALL : repeat == 2 ? RepeatType.RANDOM : repeat == 3 ? RepeatType.REPEATALL : repeat == 4 ? RepeatType.REPEATONE : null);
	}
	
	public final byte[] getPlayingSoundNameBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		String soundname = positiontracker.getPlaying(playeruuid);
		return soundname == null ? new byte[0] : soundname.getBytes(StandardCharsets.UTF_8);
	}
	
	public final byte[] getPlayingSoundSizeBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		short size = positiontracker.getPlayingSize(playeruuid);
		return size == -1 ? new byte[0] : new byte[] {(byte) size, (byte) (size >>> 8)};
	}
	
	public final byte[] getPlayingSoundRemainBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		short size = positiontracker.getPlayingRemain(playeruuid);
		return size == -1 ? new byte[0] : new byte[] {(byte) size, (byte) (size >>> 8)};
	}
	
	public final byte[] loadPackBytes(byte[] playeruuidnameupdatestatusb) {
		if(playeruuidnameupdatestatusb.length < 4) {
			return new byte[0];
		}
		int namesize = playeruuidnameupdatestatusb[0] & 0xFF, targetcount = (playeruuidnameupdatestatusb[1] & 0xFF | playeruuidnameupdatestatusb[2]<<8), flags = playeruuidnameupdatestatusb[3];
		
		final boolean update = (flags & 0x01) == 0x01, reportstatus = (flags & 0x02) == 0x02;
		
		int i = (targetcount << 4) + namesize + 4;
		if(playeruuidnameupdatestatusb.length != i) {
			return new byte[0];
		}

		byte[] nameb = new byte[namesize];
		System.arraycopy(playeruuidnameupdatestatusb, 4, nameb, 0, namesize);
		String name = new String(nameb, StandardCharsets.UTF_8);
		UUID[] playeruuids = targetcount == 0 ? null : new UUID[targetcount];
		while(--targetcount > -1) {
			long lsb = 0L, msb = 0L;
			lsb = playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb = playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			final UUID playeruuid = new UUID(msb, lsb);
			playeruuids[targetcount] = playeruuid;
		}
		if(reportstatus) {
			byte[] statusb = new byte[1];
			StatusReport statusreport = new StatusReport() {
				@Override
				public void onStatusResponse(EnumStatus status) {
					switch(status) {
					case DISPATCHED : 
						statusb[0] = 1;
					break;
					case NOTEXSIST : 
						statusb[0] = 2;
					break;
					case PACKED : 
						statusb[0] = 3;
					break;
					case REMOVED : 
						statusb[0] = 4;
					break;
					case UNAVILABLE : 
						statusb[0] = 5;
					break;
					}
				}
			};
			new ResourceFactory(name, playeruuids, datamanager, resourcemanager, update, statusreport).run();
			
			return statusb;
		}
		this.serverexecutor.execute(new ResourceFactory(name, playeruuids, datamanager, resourcemanager, update, null));
		return new byte[0];
	}
	
	public final byte[] getPackName(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		ResourcepackInfo info = positiontracker.getResourcepackInfo(playeruuid);
		String playlistname;
		return info == null || (playlistname = info.getPackname()) == null ? new byte[0] : playlistname.getBytes(StandardCharsets.UTF_8);
	}
	
	public final void stopSoundBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return;
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidb[0x08] & 0xFF;
		msb = playeruuidb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		positiontracker.stopMusic(playeruuid);
	}
	
	public final void playSoundBytes(byte[] playeruuidnameb) {
		if(playeruuidnameb.length < 0x10 || playeruuidnameb.length > 0x10F) {
			return;
		}
		long lsb = 0L, msb = 0L;
		lsb = playeruuidnameb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= playeruuidnameb[0x08] & 0xFF;
		msb = playeruuidnameb[0x07] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x06] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x05] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x04] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x03] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x02] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x01] & 0xFF;
		msb<<=8;
		msb |= playeruuidnameb[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		/*byte[] nameb = new byte[playeruuidnameb.length - 0x10];
		System.arraycopy(playeruuidnameb, 0x10, nameb, 0, nameb.length);
		String name = new String(nameb, StandardCharsets.UTF_8);*/
		String name = new String(playeruuidnameb, 0x10, playeruuidnameb.length - 0x10, StandardCharsets.UTF_8);
		positiontracker.playMusic(playeruuid, name);
	}
	
	@Override
	public void run() {
		while (run) {
			try {
				server = connectserverfactory.createServerSocket(port, backlog, hostip);
				server.setSoTimeout(timeout);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				Socket connected = null;
				try {
					connected = server.accept();
					connected.setSoTimeout(timeout);
					InetAddress connectedaddress = connected.getInetAddress();
					if (this.remoteip == null || this.remoteip.equals(connectedaddress)) {
						final Socket fconnected = connected;
						Runnable r = new Runnable() {
							@Override
							public void run() {
								try {
									processConnection(fconnected);
									fconnected.close();
								} catch(SocketException | SSLException e) {
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						};
						this.serverexecutor.execute(r);
					}
				} catch (SocketTimeoutException e) {
					continue;
				} catch (SocketException e) {
					break;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
				}
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public final void processGetResourcepackInfoName(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x100];
		int read = is.read(buf, 0, buf.length);
		final int resourcepacknamelength = buf[0x00] & 0xFF;
		if(read < resourcepacknamelength) {
			return;
		}
		String resourcepackname = new String(buf, 0, resourcepacknamelength, StandardCharsets.UTF_8);
		final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
		ResourcepackInfo info;
		if(dataentry == null || (info = dataentry.info) == null) {
			return;
		}
		ResourcepackInfo.serialize(os, info);
	}
	
	public final void processGetResourcepackInfoUUID(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x10];
		is.read(buf, 0, buf.length);
		long lsb = 0L, msb = 0L;
		lsb = buf[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x09] & 0xFF;
		lsb<<=8;
		lsb |= buf[0x08] & 0xFF;
		msb = buf[0x07] & 0xFF;
		msb<<=8;
		msb |= buf[0x06] & 0xFF;
		msb<<=8;
		msb |= buf[0x05] & 0xFF;
		msb<<=8;
		msb |= buf[0x04] & 0xFF;
		msb<<=8;
		msb |= buf[0x03] & 0xFF;
		msb<<=8;
		msb |= buf[0x02] & 0xFF;
		msb<<=8;
		msb |= buf[0x01] & 0xFF;
		msb<<=8;
		msb |= buf[0x00] & 0xFF;
		final UUID playeruuid = new UUID(msb, lsb);
		ResourcepackInfo info = positiontracker.getResourcepackInfo(playeruuid);
		ResourcepackInfo.serialize(os, info);
	}
	
	public final void processSetResourcepackCustomData(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x03];
		is.read(buf, 0, buf.length);
		int resourcepacknamelength = buf[0] & 0xFF;
		int customdatalength = (buf[1] & 0xFF | buf[2]<<8);
		buf = new byte[resourcepacknamelength];
		is.read(buf, 0, buf.length);
		String resourcepackname = new String(buf, 0, resourcepacknamelength, StandardCharsets.UTF_8);
		buf = new byte[customdatalength];
		is.read(buf, 0, buf.length);
		final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
		if(dataentry == null || !(dataentry instanceof CustomDatastore)) {
			os.write(0x00);
			return;
		}
		os.write(((CustomDatastore)dataentry).updateCustomdata(buf) ? 0x01 : 0x00);
	}
	
	private final void processConnection(Socket connected) throws IOException {
		InputStream is = connected.getInputStream();
		byte[] ibuf = new byte[9];
		if(is.read(ibuf) != 9 || ibuf[0] != 'a' || ibuf[1] != 'm' || ibuf[2] != 'r' || ibuf[3] != 'a' || ibuf[4] != 0 || ibuf[7] != 0) {
			return;
		}
		final byte packetid = ibuf[8];
		ibuf = null;
		
		if(packetid == 0x14) {
			processGetResourcepackInfoName(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x15) {
			processGetResourcepackInfoUUID(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x16) {
			processSetResourcepackCustomData(is, connected.getOutputStream());
			connected.close();
			return;
		}
		
		if(packetid == 0x07) {
			ibuf = new byte[0x11];
			is.read(ibuf);
		} else if(packetid == 0x04 || packetid == 0x06 || packetid == 0x08 || packetid == 0x09 || packetid == 0x0A || packetid == 0x0C || packetid == 0x0D) {
			ibuf = new byte[0x10];
			is.read(ibuf);
		} else if(packetid == 0x02) {
			ibuf = new byte[0x01];
			is.read(ibuf);
		} else {
			ibuf = new byte[4];
			is.read(ibuf);
			int length = (0xFF & ibuf[3]) << 24 | (0xFF & ibuf[2]) << 16 | (0xFF & ibuf[1]) << 8 | 0xFF & ibuf[0];
			ibuf = new byte[length];
			if(length != is.read(ibuf)) {
				return;
			}
		}
		ByteArraysOutputStream baos = new ByteArraysOutputStream(2);
		byte[] sizeb,obuf = null;
		int size = 0;
		switch(packetid) {
		case 0x01:
			obuf = this.getPlayersLoadedBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte)size;
			size>>>=8;
			sizeb[1] = (byte)size;
			size>>>=8;
			sizeb[2] = (byte)size;
			size>>>=8;
			sizeb[3] = (byte)size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x02:
			obuf = this.getPlaylistsBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte)size;
			size>>>=8;
			sizeb[1] = (byte)size;
			size>>>=8;
			sizeb[2] = (byte)size;
			size>>>=8;
			sizeb[3] = (byte)size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x03:
			obuf = this.getPlaylistSoundnamesPlaylistnameBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte) size;
			size >>>= 8;
			sizeb[1] = (byte) size;
			size >>>= 8;
			sizeb[2] = (byte) size;
			size >>>= 8;
			sizeb[3] = (byte) size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x04:
			obuf = this.getPlaylistSoundnamesPlayeruuidBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte) size;
			size >>>= 8;
			sizeb[1] = (byte) size;
			size >>>= 8;
			sizeb[2] = (byte) size;
			size >>>= 8;
			sizeb[3] = (byte) size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x05:
			obuf = this.getPlaylistSoundlengthsPlaylistnameBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte) size;
			size >>>= 8;
			sizeb[1] = (byte) size;
			size >>>= 8;
			sizeb[2] = (byte) size;
			size >>>= 8;
			sizeb[3] = (byte) size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x06:
			obuf = this.getPlaylistSoundlengthsPlayeruuidBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte) size;
			size >>>= 8;
			sizeb[1] = (byte) size;
			size >>>= 8;
			sizeb[2] = (byte) size;
			size >>>= 8;
			sizeb[3] = (byte) size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x07:
			this.setRepeatModeBytes(ibuf);
		break;
		case 0x08:
			obuf = this.getPlayingSoundNameBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte)size;
			size>>>=8;
			sizeb[1] = (byte)size;
			size>>>=8;
			sizeb[2] = (byte)size;
			size>>>=8;
			sizeb[3] = (byte)size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x09:
			baos.write(this.getPlayingSoundSizeBytes(ibuf));
		break;
		case 0x0A:
			baos.write(this.getPlayingSoundRemainBytes(ibuf));
		break;
		case 0x0B:
			baos.write(this.loadPackBytes(ibuf));
		break;
		case 0x0C:
			obuf = this.getPackName(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte)size;
			size>>>=8;
			sizeb[1] = (byte)size;
			size>>>=8;
			sizeb[2] = (byte)size;
			size>>>=8;
			sizeb[3] = (byte)size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x0D:
			this.stopSoundBytes(ibuf);
		break;
		case 0x0F:
			this.playSoundBytes(ibuf);
		break;
		}
		baos.writeTo(connected.getOutputStream());
		baos.close();
	}
	
	/*private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
	private static String bytesToHex(byte[] bytes) {
		int i = bytes.length, j = i << 1;
	    byte[] hexChars = new byte[j];
	    while(--i > -1) {
	    	int v = bytes[i] & 0xFF;
	        hexChars[--j] = HEX_ARRAY[v & 0x0F];
	    	hexChars[--j] = HEX_ARRAY[v >>> 4];
	    }
	    return new String(hexChars, StandardCharsets.US_ASCII);
	}
	
	private static String byteToHex(byte b) {
	    byte[] hexChars = new byte[2];
	    int v = b & 0xFF;
        hexChars[0] = HEX_ARRAY[v >>> 4];
        hexChars[1] = HEX_ARRAY[v & 0x0F];
	    return new String(hexChars, StandardCharsets.US_ASCII);
	}*/

}
