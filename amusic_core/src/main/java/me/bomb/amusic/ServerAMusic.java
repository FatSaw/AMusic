package me.bomb.amusic;

import java.io.EOFException;
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

import me.bomb.amusic.api.RepeatType;
import me.bomb.amusic.resourcepack.CustomDatastore;
import me.bomb.amusic.resourcepack.Data;
import me.bomb.amusic.resourcepack.DataEntry;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.resourcepack.SoundSource;
import me.bomb.amusic.resourcepack.SourceEntry;
import me.bomb.amusic.resourcepack.UpdateResult;
import me.bomb.amusic.resourcepack.server.ResourceManager;
import me.bomb.amusic.tracker.PositionTracker;
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
		this.logger.info("AMusic connect server started!");
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
		this.logger.info("AMusic connect server stopped!");
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
	
	public final void processGetListResourcepackInfo(InputStream is, OutputStream os) throws IOException {
		String[] resourcepackinfonames = datamanager.listResourcepacks();
		int i = resourcepackinfonames.length;
		if(i > 0xFFFF) {
			i = 0xFFFF;
		}
		byte[] buf = new byte[i], buf1 = buf;
		if(i < 2) {
			buf1 = new byte[0x02];
		}
		buf1[0] = (byte) i;
		buf1[1] = (byte) (i >>> 1);
		os.write(buf1, 0, 0x02);
		byte[][] strsbytes = new byte[i][];
		while(--i > -1) {
			byte[] resourcepackinfonamebytes = resourcepackinfonames[i].getBytes(StandardCharsets.UTF_8);
			int length = resourcepackinfonamebytes.length;
			if(length > 0xFF) {
				length = 0xFF;
			}
			buf[i] = (byte) length;
			strsbytes[i] = resourcepackinfonamebytes;
		}
		os.write(buf, 0, buf.length);
		i = buf.length;
		while(--i > -1) {
			os.write(strsbytes[i], 0, buf[i]);
		}
	}
	
	public final void processGetListResourcepack(InputStream is, OutputStream os) throws IOException {
		String[] resourcepacknames = soundsource.listResourcepacks();
		int i = resourcepacknames.length;
		if(i > 0xFFFF) {
			i = 0xFFFF;
		}
		byte[] buf = new byte[i], buf1 = buf;
		if(i < 2) {
			buf1 = new byte[0x02];
		}
		buf1[0] = (byte) i;
		buf1[1] = (byte) (i >>> 1);
		os.write(buf1, 0, 0x02);
		byte[][] strsbytes = new byte[i][];
		while(--i > -1) {
			byte[] resourcepackinfonamebytes = resourcepacknames[i].getBytes(StandardCharsets.UTF_8);
			int length = resourcepackinfonamebytes.length;
			if(length > 0xFF) {
				length = 0xFF;
			}
			buf[i] = (byte) length;
			strsbytes[i] = resourcepackinfonamebytes;
		}
		os.write(buf, 0, buf.length);
		i = buf.length;
		while(--i > -1) {
			os.write(strsbytes[i], 0, buf[i]);
		}
	}
	
	public final void processGetResourcepackInfoName(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0xFF];
		int off = 0;
		while(off < 1) {
			int n = is.read(buf, off, 1 - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		final int length = 0xFF & buf[0];
		off = 0;
		while(off < length) {
			int n = is.read(buf, off, length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String resourcepackname = new String(buf, 0, length, StandardCharsets.UTF_8);
		final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
		ResourcepackInfoImpl info;
		if(dataentry == null || (info = dataentry.info) == null) {
			return;
		}
		ResourcepackInfoImpl.serialize(os, info);
	}
	
	public final void processGetResourcepackInfoUUID(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x10];
		int off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
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
		ResourcepackInfoImpl info = positiontracker.getResourcepackInfo(playeruuid);
		ResourcepackInfoImpl.serialize(os, info);
	}
	
	public final void processSetResourcepackCustomData(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x03];
		int off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		int resourcepacknamelength = buf[0] & 0xFF;
		int customdatalength = (buf[1] & 0xFF | buf[2]<<8);
		buf = new byte[resourcepacknamelength];
		off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String resourcepackname = new String(buf, 0, resourcepacknamelength, StandardCharsets.UTF_8);
		buf = new byte[customdatalength];
		off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		final DataEntry dataentry = datamanager.getResourcepack(resourcepackname);
		if(dataentry == null || !(dataentry instanceof CustomDatastore)) {
			os.write(0x00);
			return;
		}
		os.write(((CustomDatastore)dataentry).updateCustomdata(buf) ? 0x01 : 0x00);
	}
	
	public final void processGetListResourcepackSounds(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0xFF];
		int off = 0;
		while(off < 1) {
			int n = is.read(buf, off, 1 - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		final int resourcepacknamelength = 0xFF & buf[0x00];
		off = 0;
		while(off < resourcepacknamelength) {
			int n = is.read(buf, off, resourcepacknamelength - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String resourcepackname = new String(buf, 0, resourcepacknamelength, StandardCharsets.UTF_8);
		String[] resourcepacknames = soundsource.getSounds(resourcepackname);
		if(resourcepacknames == null) {
			buf[0] = 0x00;
			buf[1] = 0x00;
			os.write(buf, 0, 0x02);
			return;
		}
		int i = resourcepacknames.length;
		if(i > 0xFFFF) {
			i = 0xFFFF;
		}
		final int length = i;
		buf[0] = (byte) length;
		buf[1] = (byte) (length >> 1);
		os.write(buf, 0, 0x02);
		if(length == 0) {
			return;
		}
		if(length > 0xFF) {
			buf = new byte[length];
		}
		byte[][] strsbytes = new byte[length][];
		while(--i > -1) {
			byte[] resourcepacknamesbytes = resourcepacknames[i].getBytes(StandardCharsets.UTF_8);
			int nameslength = resourcepacknamesbytes.length;
			if(nameslength > 0xFF) {
				nameslength = 0xFF;
			}
			buf[i] = (byte) nameslength;
			strsbytes[i] = resourcepacknamesbytes;
		}
		os.write(buf, 0, length);
		i = length;
		while(--i > -1) {
			os.write(strsbytes[i], 0, buf[i]);
		}
	}
	
	public final void processGetPackName(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x10];
		int off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
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
		ResourcepackInfoImpl info = positiontracker.getResourcepackInfo(playeruuid);
		String playlistname;
		byte[] playlistnameb;
		int length;
		if(info == null || (playlistname = info.getPackname()) == null || (length = (playlistnameb = playlistname.getBytes(StandardCharsets.UTF_8)).length) == 0x00) {
			os.write(0x00);
			return;
		}
		if(length > 0xFF) {
			length = 0xFF;
		}
		os.write(length);
		os.write(playlistnameb, 0, length);
	}
	
	public final void processGetPlayersLoaded(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0xFF];
		int off = 0;
		while(off < 1) {
			int n = is.read(buf, off, 1 - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		final int length = 0xFF & buf[0];
		off = 0;
		while(off < length) {
			int n = is.read(buf, off, length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String playlistname = new String(buf, 0, length, StandardCharsets.UTF_8);
		UUID[] playeruuids = positiontracker.getPlayersLoaded(playlistname);
		if(playeruuids == null) {
			playeruuids = new UUID[0];
		}
		
		int i = playeruuids.length, j = 1;
		if(i > 0xFFFF) {
			i = 0xFFFF;
		}
		byte[] response = new byte[2 + (i<<4)];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
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
	}
	
	public final void processStopSound(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x10];
		int off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
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
		boolean success = positiontracker.stopMusic(playeruuid);
		buf[0] = (byte) (success ? 1 : 0);
		os.write(buf, 0, 1);
	}
	
	public final void processPlaySound(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0xFF];
		int off = 0;
		while(off < 0x11) {
			int n = is.read(buf, off, 0x11 - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		int length = 0xFF & buf[0x10];
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
		off = 0;
		while(off < length) {
			int n = is.read(buf, off, length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String name = new String(buf, 0, length, StandardCharsets.UTF_8);
		boolean success = positiontracker.playMusic(playeruuid, name);
		buf[0] = (byte) (success ? 1 : 0);
		os.write(buf, 0, 1);
	}
	
	public final void processLoadPack(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0xFF];
		int off = 0;
		while(off < 0x04) {
			int n = is.read(buf, off, 0x04 - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		int namesize = buf[0];
		int targetcount = 0xFF & buf[1] | 0xFF & buf[2] << 8;
		byte flags = buf[3];
		final boolean update = (flags & 0x01) == 0x01, reportstatus = (flags & 0x02) == 0x02;
		off = 0;
		while(off < namesize) {
			int n = is.read(buf, off, namesize - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		String name = new String(buf, 0, namesize, StandardCharsets.UTF_8);
		UUID[] playeruuid = null;
		if(targetcount > 0) {
			playeruuid = new UUID[targetcount];
			int i = targetcount >> 4;
			++i;
			while(--i > -1) {
				int j = targetcount;
				if(j > 0x10) {
					j = 0x10;
				}
				int rbl = j << 4;
				off = 0;
				while(off < rbl) {
					int n = is.read(buf, off, rbl - off);
					if (n == -1) {
				        throw new EOFException();
				    }
					off += n;
				}
				while(--j > -1) {
					long msb = (buf[--off] & 0xFFL) | (buf[--off] & 0xFFL) << 8 | (buf[--off] & 0xFFL) << 16 | (buf[--off] & 0xFFL) << 24 | (buf[--off] & 0xFFL) << 32 | (buf[--off] & 0xFFL) << 40 | (buf[--off] & 0xFFL) << 48 | (buf[--off] & 0xFFL) << 56, lsb = (buf[--off] & 0xFFL) | (buf[--off] & 0xFFL) << 8 | (buf[--off] & 0xFFL) << 16 | (buf[--off] & 0xFFL) << 24 | (buf[--off] & 0xFFL) << 32 | (buf[--off] & 0xFFL) << 40 | (buf[--off] & 0xFFL) << 48 | (buf[--off] & 0xFFL) << 56;
					playeruuid[--targetcount] = new UUID(msb, lsb);
				}
			}
		}
		
		if(update) {
			UpdateResult result = datamanager.update(name);
			switch(result) {
			case UNAVILABLE:
				if(reportstatus) {
					buf[0] = 5;
					os.write(buf, 0, 0x01);
				}
				return;
			case DELETED_FAILED:
				if(reportstatus) {
					buf[0] = 2;
					os.write(buf, 0, 0x01);
				}
				return;
			case DELETED_SUCCESS:
				if(reportstatus) {
					buf[0] = 4;
					os.write(buf, 0, 0x01);
				}
				return;
			case PACKED_FAILED:
				if(reportstatus) {
					buf[0] = 2;
					os.write(buf, 0, 0x01);
				}
				return;
			case PACKED_SUCCESS:
				if(playeruuid == null) {
					if(reportstatus) {
						buf[0] = 3;
						os.write(buf, 0, 0x01);
					}
				} else {
					DataEntry dataentry = datamanager.getResourcepack(name);
					if(resourcemanager.dispatch(dataentry, playeruuid)) {
						if(reportstatus) {
							buf[0] = 1;
							os.write(buf, 0, 0x01);
						}
					} else {
						if(reportstatus) {
							buf[0] = 5;
							os.write(buf, 0, 0x01);
						}
					}
				}
				return;
			}
			return;
		}
		DataEntry dataentry = datamanager.getResourcepack(name);
		if(dataentry == null) {
			if(!reportstatus) {
				return;
			}
			buf[0] = 2;
			os.write(buf, 0, 0x01);
			return;
		}
		if(playeruuid == null) {
			if(!reportstatus) {
				return;
			}
			buf[0] = 3;
			os.write(buf, 0, 0x01);
			return;
		}
		if(resourcemanager.dispatch(dataentry, playeruuid)) {
			if(!reportstatus) {
				return;
			}
			buf[0] = 1;
			os.write(buf, 0, 0x01);
			return;
		}
		if(!reportstatus) {
			return;
		}
		buf[0] = 5;
		os.write(buf, 0, 0x01);
	}
	
	public final void processSetRepeatMode(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[0x11];
		int off = 0;
		while(off < buf.length) {
			int n = is.read(buf, off, buf.length - off);
			if (n == -1) {
		        throw new EOFException();
		    }
			off += n;
		}
		byte repeat = buf[0x10];
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
		positiontracker.setRepeater(playeruuid, repeat == 1 ? RepeatType.PLAYALL : repeat == 2 ? RepeatType.RANDOM : repeat == 3 ? RepeatType.REPEATALL : repeat == 4 ? RepeatType.REPEATONE : null);
	}
	
	private final void processConnection(Socket connected) throws IOException {

		InputStream is = connected.getInputStream();
		final byte packetid;
		{
			byte[] buf = new byte[0x09];
			int off = 0;
			while(off < buf.length) {
				int n = is.read(buf, off, buf.length - off);
				if (n == -1) {
			        throw new EOFException();
			    }
				off += n;
			}
			if(buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'r' || buf[3] != 'a' || buf[4] != 0 || buf[7] != 0) {
				return;
			}
			packetid = buf[8];
		}
		
		if(packetid == 0x14) {//
			processGetResourcepackInfoName(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x15) {//
			processGetResourcepackInfoUUID(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x16) {//
			processSetResourcepackCustomData(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x17) {//
			processGetListResourcepackInfo(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x18) {//
			processGetListResourcepack(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x19) {//
			processGetListResourcepackSounds(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1a) {//
			processGetPackName(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1b) {//
			processGetPlayersLoaded(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1c) {//
			processStopSound(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1d) {//
			processPlaySound(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1e) {//
			processLoadPack(is, connected.getOutputStream());
			connected.close();
			return;
		}
		if(packetid == 0x1f) {//
			processSetRepeatMode(is, connected.getOutputStream());
			connected.close();
			return;
		}
		connected.close();
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
