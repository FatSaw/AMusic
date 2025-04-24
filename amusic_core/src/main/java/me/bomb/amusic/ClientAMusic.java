package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.net.SocketFactory;

import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.util.ByteArraysOutputStream;

public final class ClientAMusic implements AMusic {
	
	private final InetAddress hostip, remoteip;
	private final int port;
	private final SocketFactory socketfactory;
	
	public ClientAMusic(Configuration config) {
		this.hostip = config.connectifip;
		this.remoteip = config.connectremoteip;
		this.port = config.connectport;
		this.socketfactory = config.connectsocketfactory;
	}
	
	private byte[] sendPacket(byte packetid, byte[] buf, boolean sendsize, int responsesize, boolean remotesize) {
		Socket socket = null;
		boolean fail = false;
		try {
			ByteArraysOutputStream baos = new ByteArraysOutputStream(3);
			baos.write(new byte[] {'a','m','r','a', 0, 0, 0, 0, packetid}); //PROTOCOLID
			if(buf!=null) {
				if(sendsize) {
					byte[] sizeb = new byte[4];
					int size = buf.length;
					sizeb[0] = (byte)size;
					size>>=8;
					sizeb[1] = (byte)size;
					size>>=8;
					sizeb[2] = (byte)size;
					size>>=8;
					sizeb[3] = (byte)size;
					baos.write(sizeb);
				}
				baos.write(buf);
				buf = null;
			}
			socket = socketfactory.createSocket(remoteip, port, hostip, 0);
			socket.setSoTimeout(5000);
			baos.writeTo(socket.getOutputStream());
			//baos.close();
			InputStream is = socket.getInputStream();
			if(remotesize) {
				buf = new byte[4];
				is.read(buf);
				responsesize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
			}
			if(responsesize > 65535) {
				responsesize = 65535;
			}
			
			if(responsesize > 0) {
				buf = new byte[responsesize];
				is.read(buf);
			}
		} catch (SocketTimeoutException e) {
			fail = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		return fail ? new byte[0] : buf;
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
		buf = this.sendPacket((byte)0x01, buf, true, 0, true);
		int count = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 4 + (count << 4);
		UUID[] players = new UUID[count];
		while(--count > -1) {
			long lsb = 0L, msb = 0L;
			lsb = buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			msb = buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			final UUID player = new UUID(msb, lsb);
			players[count] = player;
		}
		return players;
	}

	@Override
	public String[] getPlaylists() {
		byte[] buf = this.sendPacket((byte)0x02, null, false, 0, true);
		if(buf.length < 2) {
			return null;
		}
		int count = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
		String[] names = new String[count];
		int i = 2 + count, pos = i;
		while(--count > -1) {
			short namelength = (short) (buf[--i] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[count] = new String(nameb, StandardCharsets.UTF_8);
		}
		return names;
	}

	@Override
	public String[] getPlaylistSoundnames(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x03, buf, true, 0, true);
		int count = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
		String[] names = new String[count];
		int i = 2 + count, pos = i;
		while(--count > -1) {
			short namelength = (short) (buf[--i] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[count] = new String(nameb, StandardCharsets.UTF_8);
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
		buf = this.sendPacket((byte)0x04, buf, false, 0, true);
		int count = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
		String[] names = new String[count];
		int i = 2 + count, pos = i;
		while(--count > -1) {
			short namelength = (short) (buf[--i] & 0xFF);
			byte[] nameb = new byte[namelength];
			System.arraycopy(buf, pos, nameb, 0, namelength);
			pos+=namelength;
			names[count] = new String(nameb, StandardCharsets.UTF_8);
		}
		return names;
	}

	@Override
	public short[] getPlaylistSoundlengths(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x05, buf, true, 0, true);
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
		buf = this.sendPacket((byte)0x06, buf, false, 0, true);
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
		this.sendPacket((byte)0x07, buf, false, 0, false);
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
		buf = this.sendPacket((byte)0x08, buf, false, 0, true);
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
		buf = this.sendPacket((byte)0x09, buf, false, 2, false);
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
		buf = this.sendPacket((byte)0x0A, buf, false, 2, false);
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
		byte[] buf = new byte[4 + namelength + (playeruuid == null ? 0 : playeruuid.length << 4)];
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
		if(playeruuid == null) {
			buf[1] = 0;
			buf[2] = 0;
		} else {
			int uuidcount = playeruuid.length;
			buf[1] = (byte) uuidcount;
			buf[2] = (byte) (uuidcount >> 8);
			int j = 3 + namelength;
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
			buf = this.sendPacket((byte)0x0B, buf, true, 1, false);
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
			buf = this.sendPacket((byte)0x0B, buf, true, 0, false);
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
		buf = this.sendPacket((byte)0x0C, buf, false, 0, true);
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
		this.sendPacket((byte)0x0D, buf, false, 0, false);
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
		this.sendPacket((byte)0x0E, buf, false, 0, false);
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
		this.sendPacket((byte)0x0F, buf, true, 0, false);
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
		this.sendPacket((byte)0x10, buf, true, 0, false);
	}

	@Override
	public UUID openUploadSession(String playlistname) {
		if(playlistname == null) {
			return null;
		}
		byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
		buf = this.sendPacket((byte)0x11, buf, true, 16, false);
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
		return playeruuid;
	}

	@Override
	public UUID[] getUploadSessions() {
		byte[] buf = this.sendPacket((byte)0x12, null, false, 0, true);
		int count = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0], i = 4 + (count << 4);
		UUID[] tokens = new UUID[count];
		while(--count > -1) {
			long lsb = 0L, msb = 0L;
			lsb = buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			lsb<<=8;
			lsb |= buf[--i] & 0xFF;
			msb = buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			msb<<=8;
			msb |= buf[--i] & 0xFF;
			final UUID token = new UUID(msb, lsb);
			tokens[count] = token;
		}
		return tokens;
	}

	@Override
	public boolean closeUploadSession(UUID token, boolean save) {
		long msb = token.getMostSignificantBits(), lsb = token.getLeastSignificantBits();
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
		buf[0x10] = (byte) (save ? 1 : 0);
		buf = this.sendPacket((byte)0x13, buf, false, 1, false);
		return buf[0] == 1;
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
