package me.bomb.amusic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.source.SoundSource;

public final class ServerAMusic extends LocalAMusic implements Runnable {
	private final InetAddress hostip, remoteip;
	private final int port, backlog;
	private volatile boolean run;
	private ServerSocket server;

	public ServerAMusic(Configuration config, SoundSource source, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object, InetAddress> playerips) {
		super(config, source, packsender, soundstarter, soundstopper, playerips);
		this.hostip = config.connectifip;
		this.remoteip = config.connectremoteip;
		this.port = config.connectport;
		this.backlog = config.connectbacklog;
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
		if(playlistnameb.length > 255) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, StandardCharsets.UTF_8);
		UUID[] playeruuids = positiontracker.getPlayersLoaded(playlistname);
		int i = playeruuids.length, j = 3;
		byte[] response = new byte[4 + (i<<4)];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
		response[2] = (byte) (i>>16);
		response[3] = (byte) (i>>24);
		while(--i > -1) {
			UUID playeruuid = playeruuids[i];
			long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
		}
		return response;
	}
	
	public final byte[] getPlaylistsBytes() {
		String[] playlists = datamanager.getPlaylists();
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
		response[1] = (byte) (i>>8);
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
	
	public final byte[] getPlaylistSoundnamesPlaylistnameBytes(byte[] playlistnameb) {
		if(playlistnameb.length > 255) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, StandardCharsets.UTF_8);
		SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
		if(soundinfos==null) {
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
		response[1] = (byte) (i>>8);
		System.arraycopy(lengths, 0, response, 2, i);
		int j = i;
		while(--i > -1) {
			byte[] name = anames[i];
			int length = name.length;
			System.arraycopy(name, 0, response, j, length);
			j+=length;
		}
		return response;
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
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
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
			lengths[i] = (byte) length;
			anames[i] = namebytes;
			totallengths += length;
		}
		i = soundcount;
		byte[] response = new byte[2 + i + totallengths];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
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
		if(playlistnameb.length > 255) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, StandardCharsets.UTF_8);
		SoundInfo[] soundinfos = datamanager.getPlaylist(playlistname).sounds;
		if(soundinfos==null) {
			return new byte[0];
		}
		int soundcount = soundinfos.length;
		if(soundcount > 65535) {
			soundcount = 65535;
		}
		int i = soundcount, j = 2 + (i << 1);
		byte[] response = new byte[j];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
		while(--i > -1) {
			short length = soundinfos[i].length;
			response[--j] = (byte) length;
			length>>=8;
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
		SoundInfo[] soundinfos = positiontracker.getSoundInfo(playeruuid);
		if(soundinfos==null) {
			return new byte[0];
		}
		int soundcount = soundinfos.length;
		if(soundcount > 65535) {
			soundcount = 65535;
		}
		int i = soundcount, j = 2 + (i << 1);
		byte[] response = new byte[j];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
		while(--i > -1) {
			short length = soundinfos[i].length;
			response[--j] = (byte) length;
			length>>=8;
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
		return size == -1 ? new byte[0] : new byte[] {(byte) size, (byte) (size >> 8)};
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
		return size == -1 ? new byte[0] : new byte[] {(byte) size, (byte) (size >> 8)};
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
			new ResourceFactory(name, playeruuids, datamanager, dispatcher, source, update, statusreport, false);
			
			return statusb;
		}
		new ResourceFactory(name, playeruuids, datamanager, dispatcher, source, update, null, true);
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
		String playlistname = positiontracker.getPlaylistName(playeruuid);
		return playlistname == null ? new byte[0] : playlistname.getBytes(StandardCharsets.UTF_8);
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
	
	public final void stopSoundUntrackableBytes(byte[] playeruuidb) {
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
		positiontracker.stopMusicUntrackable(playeruuid);
	}
	
	public final void playSoundBytes(byte[] playeruuidnameb) {
		if(playeruuidnameb.length < 16 || playeruuidnameb.length > 271) {
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
		byte[] nameb = new byte[playeruuidnameb.length - 16];
		System.arraycopy(playeruuidnameb, 16, nameb, 0, nameb.length);
		String name = new String(nameb, StandardCharsets.UTF_8);
		positiontracker.playMusic(playeruuid, name);
	}
	
	public final void playSoundUntrackableBytes(byte[] playeruuidnameb) {
		if(playeruuidnameb.length < 16 || playeruuidnameb.length > 271) {
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
		byte[] nameb = new byte[playeruuidnameb.length - 16];
		System.arraycopy(playeruuidnameb, 16, nameb, 0, nameb.length);
		String name = new String(nameb, StandardCharsets.UTF_8);
		positiontracker.playMusicUntrackable(playeruuid, name);
	}
	
	public final byte[] openUploadSessionBytes(byte[] playlistnameb) {
		if(uploadermanager == null || playlistnameb.length > 255) {
			return new byte[0];
		}
		String playlistname = new String(playlistnameb, StandardCharsets.UTF_8);
		UUID token = uploadermanager.startSession(playlistname);
		long msb = token.getMostSignificantBits(), lsb = token.getLeastSignificantBits();
		byte[] response = new byte[0x10];
		response[0x00] = (byte) msb;
		msb>>=8;
		response[0x01] = (byte) msb;
		msb>>=8;
		response[0x02] = (byte) msb;
		msb>>=8;
		response[0x03] = (byte) msb;
		msb>>=8;
		response[0x04] = (byte) msb;
		msb>>=8;
		response[0x05] = (byte) msb;
		msb>>=8;
		response[0x06] = (byte) msb;
		msb>>=8;
		response[0x07] = (byte) msb;
		response[0x08] = (byte) lsb;
		lsb>>=8;
		response[0x09] = (byte) lsb;
		lsb>>=8;
		response[0x0A] = (byte) lsb;
		lsb>>=8;
		response[0x0B] = (byte) lsb;
		lsb>>=8;
		response[0x0C] = (byte) lsb;
		lsb>>=8;
		response[0x0D] = (byte) lsb;
		lsb>>=8;
		response[0x0E] = (byte) lsb;
		lsb>>=8;
		response[0x0F] = (byte) lsb;
		return response;
	}
	
	public final byte[] getUploadSessionsBytes() {
		if(uploadermanager == null) {
			return new byte[0];
		}
		UUID[] sessions = uploadermanager.getSessions();
		int i = sessions.length, j = 3;
		byte[] response = new byte[4 + (i<<4)];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
		response[2] = (byte) (i>>16);
		response[3] = (byte) (i>>24);
		while(--i > -1) {
			UUID session = sessions[i];
			long msb = session.getMostSignificantBits(), lsb = session.getLeastSignificantBits();
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			msb>>=8;
			response[++j] = (byte) msb;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
			lsb>>=8;
			response[++j] = (byte) lsb;
		}
		return response;
	}
	
	public final byte[] closeUploadSessionBytes(byte[] tokensaveb) {
		if(uploadermanager == null || tokensaveb.length != 17) {
			return new byte[0];
		}
		boolean save = tokensaveb[0x10] == 1;
		long lsb = 0L, msb = 0L;
		lsb = tokensaveb[0x0F] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x0E] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x0D] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x0C] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x0B] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x0A] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x09] & 0xFF;
		lsb<<=8;
		lsb |= tokensaveb[0x08] & 0xFF;
		msb = tokensaveb[0x07] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x06] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x05] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x04] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x03] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x02] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x01] & 0xFF;
		msb<<=8;
		msb |= tokensaveb[0x00] & 0xFF;
		final UUID token = new UUID(msb, lsb);
		return new byte[] {uploadermanager.endSession(token, save) ? (byte) 1 : (byte) 0};
	}
	
	@Override
	public void run() {
		while (run) {
			try {
				server = new ServerSocket();
				server.bind(new InetSocketAddress(hostip, port), backlog);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				Socket connected = null;
				try {
					connected = server.accept();
					InetAddress connectedaddress = connected.getInetAddress();
					if (this.remoteip == null || this.remoteip.equals(connectedaddress)) {
						final Socket fconnected = connected;
						new Thread() {
							@Override
							public void run() {
								try {
									processConnection(fconnected);
									fconnected.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
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
	
	private final void processConnection(Socket connected) throws IOException {
		InputStream is = connected.getInputStream();
		byte[] ibuf = new byte[9];
		final byte packetid;
		if(is.read(ibuf) != 9 || ibuf[0] != 'a' || ibuf[1] != 'm' || ibuf[2] != 'r' || ibuf[3] != 'a' || ibuf[4] != 0 || ibuf[7] != 0 || (packetid = ibuf[8]) < 0 || packetid > 0x13 ) {
			return;
		}
		ibuf = null;
		
		if(packetid == 0x07 || packetid == 0x13) {
			ibuf = new byte[0x11];
			is.read(ibuf);
		} else if(packetid == 0x04 || packetid == 0x06 || packetid == 0x08 || packetid == 0x09 || packetid == 0x0A || packetid == 0x0C || packetid == 0x0D || packetid == 0x0E) {
			ibuf = new byte[0x10];
			is.read(ibuf);
		} else if(packetid != 0x02 && packetid != 0x12) {
			ibuf = new byte[4];
			is.read(ibuf);
			int length = (0xFF & ibuf[3]) << 24 | (0xFF & ibuf[2]) << 16 | (0xFF & ibuf[1]) << 8 | 0xFF & ibuf[0];
			ibuf = new byte[length];
			if(packetid < 0 || packetid > 0x13 || length != is.read(ibuf)) {
				return;
			}
		}
		connected.shutdownInput();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] obuf = null;
		int size = 0;
		switch(packetid) {
		case 0x01:
			obuf = this.getPlayersLoadedBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x02:
			obuf = this.getPlaylistsBytes();
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x03:
			obuf = this.getPlaylistSoundnamesPlaylistnameBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x04:
			obuf = this.getPlaylistSoundnamesPlayeruuidBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x05:
			obuf = this.getPlaylistSoundlengthsPlaylistnameBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x06:
			obuf = this.getPlaylistSoundlengthsPlayeruuidBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x07:
			this.setRepeatModeBytes(ibuf);
		break;
		case 0x08:
			obuf = this.getPlayingSoundNameBytes(ibuf);
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
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
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x0D:
			this.stopSoundBytes(ibuf);
		break;
		case 0x0E:
			this.stopSoundUntrackableBytes(ibuf);
		break;
		case 0x0F:
			this.playSoundBytes(ibuf);
		break;
		case 0x10:
			this.playSoundUntrackableBytes(ibuf);
		break;
		case 0x11:
			baos.write(this.openUploadSessionBytes(ibuf));
		break;
		case 0x12:
			obuf = this.getUploadSessionsBytes();
			size = obuf.length;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			size>>=8;
			baos.write((byte)size);
			baos.write(obuf);
		break;
		case 0x13:
			baos.write(this.closeUploadSessionBytes(ibuf));
		break;
		}
		baos.writeTo(connected.getOutputStream());
		connected.shutdownOutput();
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
