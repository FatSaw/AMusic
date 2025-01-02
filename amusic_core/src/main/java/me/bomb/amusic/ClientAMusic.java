package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.StatusReport;

public final class ClientAMusic implements AMusic {
	
	private final InetAddress ip;
	private final int port;
	
	public ClientAMusic(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	private byte[] sendPacket(byte packetid, byte[] buf, int responsesize, boolean remotesize) {
		Socket socket = null;
		try {
			socket = new Socket(ip, port);
			OutputStream os = socket.getOutputStream();
			os.write(new byte[] {'a','m','r','a', 0, 0, 0, 0}); //PROTOCOLID
			os.write(packetid);
			if(buf!=null) {
				int size = buf.length;
				os.write((byte)size);
				size>>=8;
				os.write((byte)size);
				size>>=8;
				os.write((byte)size);
				size>>=8;
				os.write((byte)size);
				os.write(buf);
				buf = null;
			}
			
			if(remotesize) {
				buf = new byte[4];
				responsesize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
			}
			InputStream is = socket.getInputStream();
			if(responsesize < 1) { //DO NOT READ
				buf = null;
			} else {
				buf = new byte[responsesize];
				if(responsesize != is.read(buf)) {
					buf = null;
				}
			}
		} catch (IOException e) {
		} finally {
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		return buf;
	}
	
	

	@Override
	public void enable() {
		
	}

	@Override
	public void disable() {
		
	}

	@Override
	public UUID[] getPlayersLoaded(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x01, buf, 0, true);
		int count = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 4 + (count << 4);
		UUID[] players = new UUID[count];
		while(--count > -1) {
			long msb = 0L, lsb = 0L;
			lsb = buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			msb = buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			final UUID player = new UUID(msb, lsb);
			players[count] = player;
		}
		return players;
	}

	@Override
	public String[] getPlaylists() {
		byte[] buf = this.sendPacket((byte)0x02, null, 0, true);
		int split = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 0, j = 4, pos = 0;
		String[] names = new String[split];
		while(i < split) {
			short namelength = (short) (buf[j] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[i] = new String(nameb, StandardCharsets.UTF_8);
			++i;
			++j;
		}
		return names;
	}

	@Override
	public String[] getPlaylistSoundnames(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x03, buf, 0, true);
		int split = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 0, j = 4, pos = 0;
		String[] names = new String[split];
		while(i < split) {
			short namelength = (short) (buf[j] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[i] = new String(nameb, StandardCharsets.UTF_8);
			++i;
			++j;
		}
		return names;
	}

	@Override
	public String[] getPlaylistSoundnames(UUID playeruuid) {
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x04, buf, 0, true);
		int split = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 0, j = 4, pos = 0;
		String[] names = new String[split];
		while(i < split) {
			short namelength = (short) (buf[j] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[i] = new String(nameb, StandardCharsets.UTF_8);
			++i;
			++j;
		}
		return names;
	}

	@Override
	public short[] getPlaylistSoundlengths(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x05, buf, 0, true);
		int soundcount = 0xFF & buf[0] | buf[1] << 8, j = 2 + (soundcount<<1);
		short[] lengths = new short[soundcount];
		while(--soundcount > -1) {
			lengths[soundcount] = (short) (buf[--j]<<8 | buf[--j] & 0xFF);
		}
		return lengths;
	}

	@Override
	public short[] getPlaylistSoundlengths(UUID playeruuid) {
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x06, buf, 0, true);
		int soundcount = 0xFF & buf[0] | buf[1] << 8, j = 2 + (soundcount<<1);
		short[] lengths = new short[soundcount];
		while(--soundcount > -1) {
			lengths[soundcount] = (short) (buf[--j]<<8 | buf[--j] & 0xFF);
		}
		return lengths;
	}

	@Override
	public void setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		if(playeruuid == null) {
			return;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x11];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf[0X10] = repeattype == null ? (byte)0 : repeattype == RepeatType.PLAYALL ? (byte)1 : repeattype == RepeatType.RANDOM ? (byte)2 : repeattype == RepeatType.REPEATALL ? (byte)3 : repeattype == RepeatType.REPEATONE ? (byte)4 : (byte)0;
		this.sendPacket((byte)0x07, buf, 0, false);
	}

	@Override
	public String getPlayingSoundName(UUID playeruuid) {
		if(playeruuid == null) {
			return null;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x08, buf, 0, true);
		return new String(buf, StandardCharsets.UTF_8);
	}

	@Override
	public short getPlayingSoundSize(UUID playeruuid) {
		if(playeruuid == null) {
			return -1;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x09, buf, 2, false);
		return (short) (buf[0] & 0xFF | buf[1]<<8);
	}

	@Override
	public short getPlayingSoundRemain(UUID playeruuid) {
		if(playeruuid == null) {
			return -1;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x0A, buf, 2, false);
		return (short) (buf[0] & 0xFF | buf[1]<<8);
	}

	@Override
	public void loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		if(name == null) {
			return;
		}
		byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
		int namelength = nameb.length;
		if(namelength > 0xFF) {
			namelength = 0xFF;
			byte[] nnameb = new byte[0xFF];
			System.arraycopy(nameb, 0, nnameb, 0, namelength);
			nameb = nnameb;
		}
		int i = playeruuid.length;
		byte[] buf = new byte[4 + (i << 4) + namelength];
		byte flags = 0;
		if(update) {
			flags |= 0x01;
		}
		final boolean hasstatusreport = statusreport != null;
		if(hasstatusreport) {
			flags |= 0x02;
		}
		buf[0] = (byte) namelength;
		buf[3] = flags;
		System.arraycopy(nameb, 0, buf, 4, namelength);
		if(playeruuid != null) {
			int uuidcount = playeruuid.length;
			buf[1] = (byte) uuidcount;
			buf[2] = (byte) (uuidcount >> 8);
			int j = 4 + namelength;
			while(--uuidcount > -1) {
				UUID uuid = playeruuid[uuidcount];
				long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				msb>>=8;
				buf[++j] = (byte) msb;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
				lsb>>=8;
				buf[++j] = (byte) lsb;
			}
			
		}
		if(hasstatusreport) {
			buf = this.sendPacket((byte)0x0B, buf, 1, false);
			byte status = buf[0];
			switch (status) {
			case 1:
				statusreport.onStatusResponse(EnumStatus.DISPATCHED);
			break;
			case 2:
				statusreport.onStatusResponse(EnumStatus.NOTEXSIST);
			break;
			case 3:
				statusreport.onStatusResponse(EnumStatus.PACKED);
			break;
			case 4:
				statusreport.onStatusResponse(EnumStatus.REMOVED);
			break;
			case 5:
				statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
			break;
			}
		} else {
			buf = this.sendPacket((byte)0x0B, buf, 0, false);
		}
	}

	@Override
	public String getPackName(UUID playeruuid) {
		if(playeruuid == null) {
			return null;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x0C, buf, 0, true);
		return new String(buf, StandardCharsets.UTF_8);
	}

	@Override
	public void stopSound(UUID playeruuid) {
		if(playeruuid == null) {
			return;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		this.sendPacket((byte)0x0D, buf, 0, false);
	}

	@Override
	public void stopSoundUntrackable(UUID playeruuid) {
		if(playeruuid == null) {
			return;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		this.sendPacket((byte)0x0E, buf, 0, false);
	}

	@Override
	public void playSound(UUID playeruuid, String name) {
		if(playeruuid == null || name == null) {
			return;
		}
		byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
		int namelength = nameb.length;
		if(namelength > 0xFF) {
			namelength = 0xFF;
			byte[] nnameb = new byte[0xFF];
			System.arraycopy(nameb, 0, nnameb, 0, namelength);
			nameb = nnameb;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10 + namelength];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		System.arraycopy(nameb, 0, buf, 0x10, namelength);
		this.sendPacket((byte)0x0F, buf, 0, false);
	}

	@Override
	public void playSoundUntrackable(UUID playeruuid, String name) {
		if(playeruuid == null || name == null) {
			return;
		}
		byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
		int namelength = nameb.length;
		if(namelength > 0xFF) {
			namelength = 0xFF;
			byte[] nnameb = new byte[0xFF];
			System.arraycopy(nameb, 0, nnameb, 0, namelength);
			nameb = nnameb;
		}
		long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
		byte[] buf = new byte[0x10 + namelength];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		System.arraycopy(nameb, 0, buf, 0x10, namelength);
		this.sendPacket((byte)0x10, buf, 0, false);
	}

	@Override
	public UUID openUploadSession(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x11, buf, 16, false);
		long msb = 0L, lsb = 0L;
		msb = buf[0x07];
		msb<<=8;
		msb += buf[0x06];
		msb<<=8;
		msb += buf[0x05];
		msb<<=8;
		msb += buf[0x04];
		msb<<=8;
		msb += buf[0x03];
		msb<<=8;
		msb += buf[0x02];
		msb<<=8;
		msb += buf[0x01];
		msb<<=8;
		msb += buf[0x00];
		lsb = buf[0x0F];
		lsb<<=8;
		lsb += buf[0x0E];
		lsb<<=8;
		lsb += buf[0x0D];
		lsb<<=8;
		lsb += buf[0x0C];
		lsb<<=8;
		lsb += buf[0x0B];
		lsb<<=8;
		lsb += buf[0x0A];
		lsb<<=8;
		lsb += buf[0x09];
		lsb<<=8;
		lsb += buf[0x08];
		final UUID playeruuid = new UUID(msb, lsb);
		return playeruuid;
	}

	@Override
	public UUID[] getUploadSessions() {
		byte[] buf = this.sendPacket((byte)0x12, null, 0, true);
		int count = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 4 + (count << 4);
		UUID[] tokens = new UUID[count];
		while(--count > -1) {
			long msb = 0L, lsb = 0L;
			lsb = buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			lsb<<=8;
			lsb += buf[--i];
			msb = buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			msb<<=8;
			msb += buf[--i];
			final UUID token = new UUID(msb, lsb);
			tokens[count] = token;
		}
		return tokens;
	}

	@Override
	public boolean closeUploadSession(UUID token) {
		long msb = token.getMostSignificantBits(), lsb = token.getLeastSignificantBits();
		byte[] buf = new byte[0x10];
		buf[0x00] = (byte) msb;
		msb>>=8;
		buf[0x01] = (byte) msb;
		msb>>=8;
		buf[0x02] = (byte) msb;
		msb>>=8;
		buf[0x03] = (byte) msb;
		msb>>=8;
		buf[0x04] = (byte) msb;
		msb>>=8;
		buf[0x05] = (byte) msb;
		msb>>=8;
		buf[0x06] = (byte) msb;
		msb>>=8;
		buf[0x07] = (byte) msb;
		buf[0x08] = (byte) lsb;
		lsb>>=8;
		buf[0x09] = (byte) lsb;
		lsb>>=8;
		buf[0x0A] = (byte) lsb;
		lsb>>=8;
		buf[0x0B] = (byte) lsb;
		lsb>>=8;
		buf[0x0C] = (byte) lsb;
		lsb>>=8;
		buf[0x0D] = (byte) lsb;
		lsb>>=8;
		buf[0x0E] = (byte) lsb;
		lsb>>=8;
		buf[0x0F] = (byte) lsb;
		buf = this.sendPacket((byte)0x13, buf, 1, false);
		return buf[0] == 1;
	}

	

}
