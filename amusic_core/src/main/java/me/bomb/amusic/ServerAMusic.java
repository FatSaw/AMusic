package me.bomb.amusic;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.bomb.amusic.packedinfo.SoundInfo;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.ResourceFactory;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.source.SoundSource;

public final class ServerAMusic extends LocalAMusic {

	public ServerAMusic(ConfigOptions configoptions, SoundSource<?> source, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object, InetAddress> playerips) {
		super(configoptions, source, packsender, soundstarter, soundstopper, playerips);
	}
	
	@Override
	public void enable() {
		super.enable();
	}

	@Override
	public void disable() {
		super.disable();
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
		int i = playlists.length;
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
			totallengths += length;
			anames[i] = namebytes;
			lengths[i] = (byte) length;
		}
		i = playlists.length;
		byte[] response = new byte[4 + i + totallengths];
		response[0] = (byte)i;
		response[1] = (byte) (i>>8);
		response[2] = (byte) (i>>16);
		response[3] = (byte) (i>>24);
		System.arraycopy(lengths, 0, response, 4, i);
		int j = i;
		while(--i > -1) {
			byte[] name = anames[i];
			int length = name.length;
			System.arraycopy(name, 0, response, j, length);
			j+=length;
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
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
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
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
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
		long msb = 0L, lsb = 0L;
		msb = playeruuidrepeatb[0x07];
		msb<<=8;
		msb += playeruuidrepeatb[0x06];
		msb<<=8;
		msb += playeruuidrepeatb[0x05];
		msb<<=8;
		msb += playeruuidrepeatb[0x04];
		msb<<=8;
		msb += playeruuidrepeatb[0x03];
		msb<<=8;
		msb += playeruuidrepeatb[0x02];
		msb<<=8;
		msb += playeruuidrepeatb[0x01];
		msb<<=8;
		msb += playeruuidrepeatb[0x00];
		lsb = playeruuidrepeatb[0x0F];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x0E];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x0D];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x0C];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x0B];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x0A];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x09];
		lsb<<=8;
		lsb += playeruuidrepeatb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		byte repeat = playeruuidrepeatb[0x10];
		positiontracker.setRepeater(playeruuid, repeat == 1 ? RepeatType.PLAYALL : repeat == 2 ? RepeatType.RANDOM : repeat == 3 ? RepeatType.REPEATALL : repeat == 4 ? RepeatType.REPEATONE : null);
	}
	
	public final byte[] getPlayingSoundNameBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		String soundname = positiontracker.getPlaying(playeruuid);
		return soundname == null ? new byte[0] : soundname.getBytes(StandardCharsets.UTF_8);
	}
	
	public final byte[] getPlayingSoundSizeBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		short size = positiontracker.getPlayingSize(playeruuid);
		return size == -1 ? new byte[0] : new byte[] {(byte) size, (byte) (size >> 8)};
	}
	
	public final byte[] getPlayingSoundRemainBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
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
		
		int j = (targetcount << 4) ;
		if(playeruuidnameupdatestatusb.length != j + namesize + 4) {
			return new byte[0];
		}
		j+=4;

		byte[] nameb = new byte[namesize];
		System.arraycopy(playeruuidnameupdatestatusb, j, nameb, 0, namesize);
		String name = new String(nameb, StandardCharsets.UTF_8);
		UUID[] playeruuids = new UUID[targetcount];
		while(--targetcount > -1) {
			long msb = 0L, lsb = 0L;
			lsb = playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			lsb<<=8;
			lsb += playeruuidnameupdatestatusb[--j];
			msb = playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
			msb<<=8;
			msb += playeruuidnameupdatestatusb[--j];
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
					this.notify();
				}
			};
			new ResourceFactory(name, playeruuids, datamanager, dispatcher, source, update, statusreport);
			try {
				statusreport.wait(30000);
			} catch (InterruptedException e) {
			}
			return statusb;
		}
		new ResourceFactory(name, playeruuids, datamanager, dispatcher, source, update, null);
		return new byte[0];
	}
	
	public final byte[] getPackName(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return new byte[0];
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		String playlistname = positiontracker.getPlaylistName(playeruuid);
		return playlistname == null ? new byte[0] : playlistname.getBytes(StandardCharsets.UTF_8);
	}
	
	public final void stopSoundBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return;
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		positiontracker.stopMusic(playeruuid);
	}
	
	public final void stopSoundUntrackableBytes(byte[] playeruuidb) {
		if(playeruuidb.length != 16) {
			return;
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidb[0x07];
		msb<<=8;
		msb += playeruuidb[0x06];
		msb<<=8;
		msb += playeruuidb[0x05];
		msb<<=8;
		msb += playeruuidb[0x04];
		msb<<=8;
		msb += playeruuidb[0x03];
		msb<<=8;
		msb += playeruuidb[0x02];
		msb<<=8;
		msb += playeruuidb[0x01];
		msb<<=8;
		msb += playeruuidb[0x00];
		lsb = playeruuidb[0x0F];
		lsb<<=8;
		lsb += playeruuidb[0x0E];
		lsb<<=8;
		lsb += playeruuidb[0x0D];
		lsb<<=8;
		lsb += playeruuidb[0x0C];
		lsb<<=8;
		lsb += playeruuidb[0x0B];
		lsb<<=8;
		lsb += playeruuidb[0x0A];
		lsb<<=8;
		lsb += playeruuidb[0x09];
		lsb<<=8;
		lsb += playeruuidb[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		positiontracker.stopMusicUntrackable(playeruuid);
	}
	
	public final void playSoundBytes(byte[] playeruuidnameb) {
		if(playeruuidnameb.length < 16 || playeruuidnameb.length > 271) {
			return;
		}
		long msb = 0L, lsb = 0L;
		msb = playeruuidnameb[0x07];
		msb<<=8;
		msb += playeruuidnameb[0x06];
		msb<<=8;
		msb += playeruuidnameb[0x05];
		msb<<=8;
		msb += playeruuidnameb[0x04];
		msb<<=8;
		msb += playeruuidnameb[0x03];
		msb<<=8;
		msb += playeruuidnameb[0x02];
		msb<<=8;
		msb += playeruuidnameb[0x01];
		msb<<=8;
		msb += playeruuidnameb[0x00];
		lsb = playeruuidnameb[0x0F];
		lsb<<=8;
		lsb += playeruuidnameb[0x0E];
		lsb<<=8;
		lsb += playeruuidnameb[0x0D];
		lsb<<=8;
		lsb += playeruuidnameb[0x0C];
		lsb<<=8;
		lsb += playeruuidnameb[0x0B];
		lsb<<=8;
		lsb += playeruuidnameb[0x0A];
		lsb<<=8;
		lsb += playeruuidnameb[0x09];
		lsb<<=8;
		lsb += playeruuidnameb[0x08];
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
		long msb = 0L, lsb = 0L;
		msb = playeruuidnameb[0x07];
		msb<<=8;
		msb += playeruuidnameb[0x06];
		msb<<=8;
		msb += playeruuidnameb[0x05];
		msb<<=8;
		msb += playeruuidnameb[0x04];
		msb<<=8;
		msb += playeruuidnameb[0x03];
		msb<<=8;
		msb += playeruuidnameb[0x02];
		msb<<=8;
		msb += playeruuidnameb[0x01];
		msb<<=8;
		msb += playeruuidnameb[0x00];
		lsb = playeruuidnameb[0x0F];
		lsb<<=8;
		lsb += playeruuidnameb[0x0E];
		lsb<<=8;
		lsb += playeruuidnameb[0x0D];
		lsb<<=8;
		lsb += playeruuidnameb[0x0C];
		lsb<<=8;
		lsb += playeruuidnameb[0x0B];
		lsb<<=8;
		lsb += playeruuidnameb[0x0A];
		lsb<<=8;
		lsb += playeruuidnameb[0x09];
		lsb<<=8;
		lsb += playeruuidnameb[0x08];
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
		UUID token = uploadermanager.generateToken(playlistname);
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
	
	public final byte[] closeUploadSessionBytes(byte[] tokenb) {
		if(uploadermanager == null || tokenb.length != 16) {
			return new byte[0];
		}
		long msb = 0L, lsb = 0L;
		msb = tokenb[0x07];
		msb<<=8;
		msb += tokenb[0x06];
		msb<<=8;
		msb += tokenb[0x05];
		msb<<=8;
		msb += tokenb[0x04];
		msb<<=8;
		msb += tokenb[0x03];
		msb<<=8;
		msb += tokenb[0x02];
		msb<<=8;
		msb += tokenb[0x01];
		msb<<=8;
		msb += tokenb[0x00];
		lsb = tokenb[0x0F];
		lsb<<=8;
		lsb += tokenb[0x0E];
		lsb<<=8;
		lsb += tokenb[0x0D];
		lsb<<=8;
		lsb += tokenb[0x0C];
		lsb<<=8;
		lsb += tokenb[0x0B];
		lsb<<=8;
		lsb += tokenb[0x0A];
		lsb<<=8;
		lsb += tokenb[0x09];
		lsb<<=8;
		lsb += tokenb[0x08];
		final UUID token = new UUID(msb, lsb);
		return new byte[] {uploadermanager.endSession(token) ? (byte) 1 : (byte) 0};
	}

}
