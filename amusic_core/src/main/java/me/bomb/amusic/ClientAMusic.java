package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.net.SocketFactory;

import me.bomb.amusic.packedinfo.ResourcepackInfo;
import me.bomb.amusic.resource.EnumStatus;
import me.bomb.amusic.resource.StatusReport;
import me.bomb.amusic.util.ByteArraysOutputStream;
import me.bomb.amusic.util.Logger;

public final class ClientAMusic implements AMusic {
	
	public final Logger logger;
	private final InetAddress hostip, remoteip;
	private final int port, timeout;
	private final SocketFactory socketfactory;
	private final Executor executor;
	
	private ResourcepackInfoCache resourcepackinfocache;
	
	public ClientAMusic(Logger logger, InetAddress hostip, InetAddress remoteip, int port, SocketFactory socketfactory, Executor executor) {
		this.logger = logger;
		this.hostip = hostip;
		this.remoteip = remoteip;
		this.port = port;
		this.timeout = 5000;
		this.socketfactory = socketfactory;
		this.executor = executor;
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
					size>>>=8;
					sizeb[1] = (byte)size;
					size>>>=8;
					sizeb[2] = (byte)size;
					size>>>=8;
					sizeb[3] = (byte)size;
					baos.write(sizeb);
				}
				baos.write(buf);
				buf = null;
			}
			socket = socketfactory.createSocket(remoteip, port, hostip, 0);
			socket.setSoTimeout(5000);
			baos.writeTo(socket.getOutputStream());
			baos.close();
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
			} else {
				buf = new byte[0];
			}
		} catch (SocketTimeoutException e) {
			fail = true;
		} catch (IOException e) {
			fail = true;
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
	public boolean updateCache() {
		this.resourcepackinfocache.interrupt();
		return true;
	}

	@Override
	public void enable() {
		ResourcepackInfoCache resourcepackinfocache = new ResourcepackInfoCache(this, 60000L, Long.MAX_VALUE);
		resourcepackinfocache.run = true;
		resourcepackinfocache.start();
		this.resourcepackinfocache = resourcepackinfocache;
		this.logger.info("AMusic connect client started!");
	}

	@Override
	public void disable() {
		ResourcepackInfoCache resourcepackinfocache = this.resourcepackinfocache;
		resourcepackinfocache.run = false;
		resourcepackinfocache.interrupt();
		this.logger.info("AMusic connect client stopped!");
	}
	
	@Override
	public void login(UUID playeruuid) {
		//TODO: Add uuid to track for cache synchronizations for uuid based get
	}
	
	@Override
	public void logout(UUID playeruuid) {
		cachePlayerPlaylistSoundnames.remove(playeruuid);
		cachePlayerPlaylistSoundlengths.remove(playeruuid);
	}

	@Override
	public boolean getPlayersLoaded(String playlistname, Consumer<UUID[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] buf = playlistname.getBytes(StandardCharsets.UTF_8);
				buf = ClientAMusic.this.sendPacket((byte)0x01, buf, true, 0, true);
				if(buf.length == 0) {
					resultConsumer.accept(null);
					return;
				}
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
				resultConsumer.accept(players);
			}
		};
		executor.execute(r);
		return true;
	}
	
	private ConcurrentHashMap<UUID, String[]> cachePlayerPlaylistSoundnames = new ConcurrentHashMap<>();

	@Override
	public boolean getPlaylistSoundnames(UUID playeruuid, boolean useCache, Consumer<String[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		if(useCache) {
			String[] soundnames = cachePlayerPlaylistSoundnames.get(playeruuid);
			if(soundnames != null) {
				resultConsumer.accept(soundnames);
				return false;
			}
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf = ClientAMusic.this.sendPacket((byte)0x04, buf, false, 0, true);
				if(buf.length == 0) {
					resultConsumer.accept(null);
					return;
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
				String[] namescache = new String[names.length];
				System.arraycopy(names, 0, namescache, 0, names.length);
				cachePlayerPlaylistSoundnames.put(playeruuid, namescache);
				resultConsumer.accept(names);
			}
		};
		executor.execute(r);
		return true;
	}
	
	private ConcurrentHashMap<UUID, short[]> cachePlayerPlaylistSoundlengths = new ConcurrentHashMap<>();

	@Override
	public boolean getPlaylistSoundlengths(UUID playeruuid, boolean useCache, Consumer<short[]> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		if(useCache) {
			short[] soundlengths = cachePlayerPlaylistSoundlengths.get(playeruuid);
			if(soundlengths != null) {
				resultConsumer.accept(soundlengths);
				return false;
			}
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf = ClientAMusic.this.sendPacket((byte)0x06, buf, false, 0, true);
				if(buf.length == 0) {
					resultConsumer.accept(null);
					return;
				}
				int soundcount = 0xFF & buf[0] | buf[1] << 8, j = 2 + (soundcount<<1);
				short[] lengths = new short[soundcount];
				while(--soundcount > -1) {
					lengths[soundcount] = (short) (buf[--j]<<8 | buf[--j] & 0xFF);
				}
				short[] lengthscache = new short[lengths.length];
				System.arraycopy(lengths, 0, lengthscache, 0, lengths.length);
				cachePlayerPlaylistSoundlengths.put(playeruuid, lengthscache);
				resultConsumer.accept(lengths);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public boolean setRepeatMode(UUID playeruuid, RepeatType repeattype) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x11];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf[0X10] = repeattype == null ? (byte)0 : repeattype == RepeatType.PLAYALL ? (byte)1 : repeattype == RepeatType.RANDOM ? (byte)2 : repeattype == RepeatType.REPEATALL ? (byte)3 : repeattype == RepeatType.REPEATONE ? (byte)4 : (byte)0;
				ClientAMusic.this.sendPacket((byte)0x07, buf, false, 0, false);
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf = ClientAMusic.this.sendPacket((byte)0x08, buf, false, 0, true);
				if(buf.length == 0) {
					resultConsumer.accept(null);
					return;
				}
				resultConsumer.accept(new String(buf, StandardCharsets.UTF_8));
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf = ClientAMusic.this.sendPacket((byte)0x09, buf, false, 2, false);
				if(buf.length == 0) {
					resultConsumer.accept((short) -1);
					return;
				}
				resultConsumer.accept((short) (buf[0] & 0xFF | buf[1]<<8));
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				buf = ClientAMusic.this.sendPacket((byte)0x0A, buf, false, 2, false);
				if(buf.length == 0) {
					resultConsumer.accept((short) -1);
					return;
				}
				resultConsumer.accept((short) (buf[0] & 0xFF | buf[1]<<8));
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport) {
		if(name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
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
					buf[2] = (byte) (uuidcount >>> 8);
					int j = 3 + namelength;
					while(--uuidcount > -1) {
						UUID uuid = playeruuid[uuidcount];
						long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						msb>>>=8;
						buf[++j] = (byte) msb;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
						lsb>>>=8;
						buf[++j] = (byte) lsb;
					}
					
				}
				if(hasstatusreport) {
					buf = ClientAMusic.this.sendPacket((byte)0x0B, buf, true, 1, false);
					byte status = buf[0];
					switch (status) {
					case 1:
						statusreport.onStatusResponse(EnumStatus.DISPATCHED);
						if(playeruuid != null) {
							int uuidcount = playeruuid.length;
							while(--uuidcount > -1) {
								UUID uuid = playeruuid[uuidcount];
								cachePlayerPlaylistSoundnames.remove(uuid);
								cachePlayerPlaylistSoundlengths.remove(uuid);
							}
						}
					break;
					case 2:
						statusreport.onStatusResponse(EnumStatus.NOTEXSIST);
					break;
					case 3:
						statusreport.onStatusResponse(EnumStatus.PACKED);
						if(update) {
							if(!resourcepackinfocache.updateResourcepackInfo(name)) {
								resourcepackinfocache.addResourcepackInfo(name);
							}
						}
					break;
					case 4:
						statusreport.onStatusResponse(EnumStatus.REMOVED);
						if(update) {
							resourcepackinfocache.removeResourcepackInfo(name);
						}
					break;
					case 5:
						statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
					break;
					}
				} else {
					buf = ClientAMusic.this.sendPacket((byte)0x0B, buf, true, 0, false);
					if(update) {
						if(!resourcepackinfocache.updateResourcepackInfo(name)) {
							if(!resourcepackinfocache.addResourcepackInfo(name)) {
								resourcepackinfocache.removeResourcepackInfo(name);
							}
						}
					}
					if(playeruuid != null) {
						int uuidcount = playeruuid.length;
						while(--uuidcount > -1) {
							UUID uuid = playeruuid[uuidcount];
							cachePlayerPlaylistSoundnames.remove(uuid);
							cachePlayerPlaylistSoundlengths.remove(uuid);
						}
					}
				}
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean stopSound(UUID playeruuid) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x10];
				buf[0x00] = (byte) msb;
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				ClientAMusic.this.sendPacket((byte)0x0D, buf, false, 0, false);
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean playSound(UUID playeruuid, String name) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
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
				msb>>>=8;
				buf[0x01] = (byte) msb;
				msb>>>=8;
				buf[0x02] = (byte) msb;
				msb>>>=8;
				buf[0x03] = (byte) msb;
				msb>>>=8;
				buf[0x04] = (byte) msb;
				msb>>>=8;
				buf[0x05] = (byte) msb;
				msb>>>=8;
				buf[0x06] = (byte) msb;
				msb>>>=8;
				buf[0x07] = (byte) msb;
				buf[0x08] = (byte) lsb;
				lsb>>>=8;
				buf[0x09] = (byte) lsb;
				lsb>>>=8;
				buf[0x0A] = (byte) lsb;
				lsb>>>=8;
				buf[0x0B] = (byte) lsb;
				lsb>>>=8;
				buf[0x0C] = (byte) lsb;
				lsb>>>=8;
				buf[0x0D] = (byte) lsb;
				lsb>>>=8;
				buf[0x0E] = (byte) lsb;
				lsb>>>=8;
				buf[0x0F] = (byte) lsb;
				System.arraycopy(nameb, 0, buf, 0x10, namelength);
				ClientAMusic.this.sendPacket((byte)0x0F, buf, true, 0, false);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public ResourcepackInfo getResourcepackInfoCached(String resourcepackname) {
		if(resourcepackname == null) {
			return null;
		}
		return this.resourcepackinfocache.resourcepacks.get(resourcepackname);
	}

	@Override
	public boolean getResourcepackInfo(String resourcepackname, Consumer<ResourcepackInfo> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
				int resourcepacknamelength = resourcepacknameb.length;
				if(resourcepacknamelength > 0xFF) {
					resourcepacknamelength = 0xFF;
				}
				
				ResourcepackInfo info = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x0a];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x14;
					buf[0x09] = (byte) resourcepacknamelength;
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfo.deserialize(is);
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(info);
			}
		};
		executor.execute(r);
		return true;
	}

	@Override
	public boolean getResourcepackInfo(UUID playeruuid, Consumer<ResourcepackInfo> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
				byte[] buf = new byte[0x19];
				buf[0x00] = 'a';
				buf[0x01] = 'm';
				buf[0x02] = 'r';
				buf[0x03] = 'a';
				buf[0x04] = 0x00;
				buf[0x05] = 0x00;
				buf[0x06] = 0x00;
				buf[0x07] = 0x00;
				buf[0x08] = 0x15;
				buf[0x09] = (byte) msb;
				msb>>>=8;
				buf[0x0a] = (byte) msb;
				msb>>>=8;
				buf[0x0b] = (byte) msb;
				msb>>>=8;
				buf[0x0c] = (byte) msb;
				msb>>>=8;
				buf[0x0d] = (byte) msb;
				msb>>>=8;
				buf[0x0e] = (byte) msb;
				msb>>>=8;
				buf[0x0f] = (byte) msb;
				msb>>>=8;
				buf[0x10] = (byte) msb;
				buf[0x11] = (byte) lsb;
				lsb>>>=8;
				buf[0x12] = (byte) lsb;
				lsb>>>=8;
				buf[0x13] = (byte) lsb;
				lsb>>>=8;
				buf[0x14] = (byte) lsb;
				lsb>>>=8;
				buf[0x15] = (byte) lsb;
				lsb>>>=8;
				buf[0x16] = (byte) lsb;
				lsb>>>=8;
				buf[0x17] = (byte) lsb;
				lsb>>>=8;
				buf[0x18] = (byte) lsb;
				ResourcepackInfo info = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					os.write(buf, 0, buf.length);
					buf = null;
					InputStream is = socket.getInputStream();
					info = ResourcepackInfo.deserialize(is);
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(info);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean setResourcepackCustomData(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer) {
		if(resourcepackname == null || customdata == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
				int resourcepacknamelength = resourcepacknameb.length;
				if(resourcepacknamelength > 0xFF) {
					resourcepacknamelength = 0xFF;
				}
				int customdatalength = customdata.length;
				if(customdatalength > 0xFFFF) {
					customdatalength = 0xFFFF;
				}
				
				Socket socket = null;
				boolean success = false;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x0c];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x16;
					buf[0x09] = (byte) resourcepacknamelength;
					buf[0x0a] = (byte) customdatalength;
					buf[0x0b] = (byte) (customdatalength >>> 8);
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					os.write(customdata, 0, customdatalength);
					InputStream is = socket.getInputStream();
					buf = new byte[1];
					is.read(buf);
					success = buf[0] == 1;
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(success);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getListResourcepackInfoCached() {
		return this.resourcepackinfocache.resourcepackinfolist;
	}

	@Override
	public final boolean getListResourcepackInfo(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				String[] names = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x09];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x17;
					os.write(buf, 0, buf.length);
					buf = new byte[0x02];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					buf = new byte[i];
					is.read(buf, 0, buf.length);
					names = new String[i];
					byte[] strbuf = new byte[0xFF];
					while(--i > -1) {
						int len = buf[i] & 0xFF;
						is.read(strbuf, 0, len);
						names[i] = new String(strbuf, 0, len, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(names);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getListResourcepackCached() {
		return this.resourcepackinfocache.resourcepacklist;
	}
	
	@Override
	public final boolean getListResourcepack(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				String[] names = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x09];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x18;
					os.write(buf, 0, buf.length);
					buf = new byte[0x02];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					//int i = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
					buf = new byte[i];
					is.read(buf, 0, buf.length);
					names = new String[i];
					byte[] strbuf = new byte[0xFF];
					while(--i > -1) {
						int len = buf[i] & 0xFF;
						is.read(strbuf, 0, len);
						names[i] = new String(strbuf, 0, len, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(names);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public String[] getListResourcepackSoundsCached(String resourcepackname) {
		return this.resourcepackinfocache.resourcepacksoundnames.get(resourcepackname);
	}
	
	@Override
	public boolean getListResourcepackSounds(String resourcepackname, Consumer<String[]> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
				int resourcepacknamelength = resourcepacknameb.length;
				if(resourcepacknamelength > 0xFF) {
					resourcepacknamelength = 0xFF;
				}
				
				String[] soundnames = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x0a];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x19;
					buf[0x09] = (byte) resourcepacknamelength;
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					buf = new byte[0x02];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					buf = new byte[i];
					is.read(buf, 0, buf.length);
					soundnames = new String[i];
					byte[] strbuf = new byte[0xFF];
					while(--i > -1) {
						int len = buf[i] & 0xFF;
						is.read(strbuf, 0, len);
						soundnames[i] = new String(strbuf, 0, len, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(soundnames);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public boolean getPackName(UUID playeruuid, Consumer<String> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				String packname = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x09];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x1a;
					os.write(buf, 0, buf.length);
					
					long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
					buf = new byte[0x10];
					buf[0x00] = (byte) msb;
					msb>>>=8;
					buf[0x01] = (byte) msb;
					msb>>>=8;
					buf[0x02] = (byte) msb;
					msb>>>=8;
					buf[0x03] = (byte) msb;
					msb>>>=8;
					buf[0x04] = (byte) msb;
					msb>>>=8;
					buf[0x05] = (byte) msb;
					msb>>>=8;
					buf[0x06] = (byte) msb;
					msb>>>=8;
					buf[0x07] = (byte) msb;
					buf[0x08] = (byte) lsb;
					lsb>>>=8;
					buf[0x09] = (byte) lsb;
					lsb>>>=8;
					buf[0x0A] = (byte) lsb;
					lsb>>>=8;
					buf[0x0B] = (byte) lsb;
					lsb>>>=8;
					buf[0x0C] = (byte) lsb;
					lsb>>>=8;
					buf[0x0D] = (byte) lsb;
					lsb>>>=8;
					buf[0x0E] = (byte) lsb;
					lsb>>>=8;
					buf[0x0F] = (byte) lsb;
					os.write(buf, 0, buf.length);
					
					buf = new byte[0x01];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					int length = 0xFF & buf[0];
					if(length > 0) {
						buf = new byte[length];
						packname = new String(buf, 0, length, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(packname);
			}
		};
		executor.execute(r);
		return true;
	}
	
	private Socket socket() throws IOException {
		Socket socket = this.socketfactory.createSocket(this.remoteip, this.port, this.hostip, 0);
		socket.setSoTimeout(this.timeout);
		return socket;
	}
	
	private void packetend(Socket socket) {
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
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
	
	public final static class ResourcepackInfoCache extends Thread {
		
		private final ClientAMusic amusic;
		private final long synchronizationdelayfailed, synchronizationdelay;
		private final HashMap<String, ResourcepackInfo> defaultresourcepacksmap = new HashMap<String, ResourcepackInfo>(0);
		private final HashMap<String, String[]> defaultsoundnamesmap = new HashMap<String, String[]>(0);
		protected String[] resourcepackinfolist = null, resourcepacklist = null;
		protected HashMap<String, ResourcepackInfo> resourcepacks = this.defaultresourcepacksmap;
		protected HashMap<String, String[]> resourcepacksoundnames = this.defaultsoundnamesmap;
		protected volatile boolean run;
		
		private ResourcepackInfoCache(ClientAMusic amusic, long synchronizationdelayfailed, long synchronizationdelay) {
			this.amusic = amusic;
			this.synchronizationdelayfailed = synchronizationdelayfailed;
			this.synchronizationdelay = synchronizationdelay;
		}
		
		@Override
		public void run() {
			while(this.run) {
				Socket socket = null;
				byte[] lengths = null;
				byte[][] namesbytes = null;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x09];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x17;
					os.write(buf, 0, buf.length);
					buf = new byte[0x02];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					lengths = new byte[i];
					is.read(lengths, 0, lengths.length);
					namesbytes = new byte[i][];
					while(--i > -1) {
						int len = lengths[i] & 0xFF;
						byte[] strbuf = new byte[len];
						is.read(strbuf, 0, len);
						namesbytes[i] = strbuf;
					}
				} catch (IOException e) {
				} finally {
					this.amusic.packetend(socket);
				}
				if(lengths == null || namesbytes == null) {
					this.amusic.logger.warn("Resourcepack info synchronization failed!");
					try {
						Thread.sleep(this.synchronizationdelayfailed);
					} catch (InterruptedException e) {
						Thread.interrupted();
					}
					continue;
				}
				int i = namesbytes.length, j = 0;
				String[] resourcepackinfolist = new String[i];
				final HashMap<String, ResourcepackInfo> resourcepacks = new HashMap<String, ResourcepackInfo>(i);
				while(--i > -1) {
					byte[] resourcepacknameb = namesbytes[i];
					byte len = lengths[i];
					ResourcepackInfo info = null;
					try {
						socket = this.amusic.socket();
						OutputStream os = socket.getOutputStream();
						byte[] buf = new byte[0x0a];
						buf[0x00] = 'a';
						buf[0x01] = 'm';
						buf[0x02] = 'r';
						buf[0x03] = 'a';
						buf[0x04] = 0x00;
						buf[0x05] = 0x00;
						buf[0x06] = 0x00;
						buf[0x07] = 0x00;
						buf[0x08] = 0x14;
						buf[0x09] = len;
						os.write(buf, 0, buf.length);
						buf = null;
						os.write(resourcepacknameb, 0, len & 0xFF);
						InputStream is = socket.getInputStream();
						info = ResourcepackInfo.deserialize(is);
						++j;
					} catch (IOException e) {
					} finally {
						this.amusic.packetend(socket);
					}
					if(info != null) {
						String resourcepackname = info.getPackname();
						resourcepackinfolist[i] = resourcepackname;
						resourcepacks.put(resourcepackname, info);
						this.amusic.logger.warn("Resourcepack added to cache (" + resourcepackname + ")");
					}
				}
				i = namesbytes.length;
				this.resourcepackinfolist = resourcepackinfolist;
				this.resourcepacks = resourcepacks;
				this.amusic.logger.info("Resourcepack info synchronized (" + Integer.toString(j) + "/" + Integer.toString(i) + ")");
				
				lengths = null;
				byte[][] resourcepacklistbytes = null;
				String[] resourcepacklist = null;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x09];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x18;
					os.write(buf, 0, buf.length);
					buf = new byte[0x02];
					InputStream is = socket.getInputStream();
					is.read(buf, 0, buf.length);
					i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					//i = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
					lengths = new byte[i];
					is.read(lengths, 0, lengths.length);
					resourcepacklistbytes = new byte[i][];
					resourcepacklist = new String[i];
					while(--i > -1) {
						int len = lengths[i] & 0xFF;
						byte[] strbuf = new byte[len];
						is.read(strbuf, 0, len);
						resourcepacklistbytes[i] = strbuf;
						resourcepacklist[i] = new String(strbuf, 0, len, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					this.amusic.packetend(socket);
				}
				this.resourcepacklist = resourcepacklist;
				HashMap<String, String[]> resourcepacksoundnames = new HashMap<String, String[]>(0);
				i = resourcepacklist.length;
				while(--i > -1) {
					byte[] resourcepacknameb = resourcepacklistbytes[i];
					byte len = lengths[i];
					
					String[] soundnames = null;
					socket = null;
					try {
						socket = this.amusic.socket();
						OutputStream os = socket.getOutputStream();
						byte[] buf = new byte[0x0a];
						buf[0x00] = 'a';
						buf[0x01] = 'm';
						buf[0x02] = 'r';
						buf[0x03] = 'a';
						buf[0x04] = 0x00;
						buf[0x05] = 0x00;
						buf[0x06] = 0x00;
						buf[0x07] = 0x00;
						buf[0x08] = 0x19;
						buf[0x09] = len;
						os.write(buf, 0, buf.length);
						buf = null;
						os.write(resourcepacknameb, 0, len & 0xFF);
						buf = new byte[0x02];
						InputStream is = socket.getInputStream();
						is.read(buf, 0, buf.length);
						j = 0xFF & buf[0] | 0xFF & buf[1] << 8;
						//j = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
						buf = new byte[j];
						is.read(buf, 0, buf.length);
						soundnames = new String[j];
						byte[] strbuf = new byte[0xFF];
						while(--j > -1) {
							int slen = buf[i] & 0xFF;
							is.read(strbuf, 0, slen);
							soundnames[j] = new String(strbuf, 0, slen, StandardCharsets.UTF_8);
						}
					} catch (IOException e) {
					} finally {
						this.amusic.packetend(socket);
					}
					resourcepacksoundnames.put(resourcepacklist[i], soundnames);
				}
				
				this.resourcepacksoundnames = resourcepacksoundnames;
				this.amusic.logger.info("Music directory list synchronized (" + Integer.toString(resourcepacklist.length) + ")");
				try {
					Thread.sleep(this.synchronizationdelay);
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
			this.resourcepackinfolist = null;
			this.resourcepacks = this.defaultresourcepacksmap;
			this.resourcepacklist = null;
			this.resourcepacksoundnames = this.defaultsoundnamesmap;
		}
		
		public boolean addResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfo> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfo oinfo = resourcepacks.get(resourcepackname);
				if(oinfo != null) {
					return false;
				}
				byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
				int resourcepacknamelength = resourcepacknameb.length;
				if(resourcepacknamelength > 0xFF) {
					resourcepacknamelength = 0xFF;
				}
				ResourcepackInfo info = null;
				Socket socket = null;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x0a];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x14;
					buf[0x09] = (byte) resourcepacknamelength;
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfo.deserialize(is);
				} catch (IOException e) {
					this.amusic.logger.warn(e.getMessage());
				} finally {
					this.amusic.packetend(socket);
				}
				if(info == null) {
					return false;
				}
				
				int i = resourcepacks.size();
				++i;
				HashMap<String, ResourcepackInfo> newresourcepacks = new HashMap<String, ResourcepackInfo>(i);
				String[] resourcepackinfolist = new String[i];
				Set<Entry<String, ResourcepackInfo>> entryset = newresourcepacks.entrySet();
				Iterator<Entry<String, ResourcepackInfo>> iterator = entryset.iterator();
				while(--i > 0 && iterator.hasNext()) {
					Entry<String, ResourcepackInfo> entry = iterator.next();
					String entrykey = entry.getKey();
					entryset.add(entry);
					resourcepackinfolist[i] = entrykey;
				}
				newresourcepacks.put(resourcepackname, info);
				resourcepackinfolist[i] = resourcepackname;

				this.resourcepacks = newresourcepacks;
				this.resourcepackinfolist = resourcepackinfolist;
			}
			this.amusic.logger.info("AMusic resourcepack info cached add (" + resourcepackname + ")");
			return true;
		}
		
		public boolean updateResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfo> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfo oinfo = resourcepacks.get(resourcepackname);
				if(oinfo == null) {
					return false;
				}
				byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
				int resourcepacknamelength = resourcepacknameb.length;
				if(resourcepacknamelength > 0xFF) {
					resourcepacknamelength = 0xFF;
				}
				ResourcepackInfo info = null;
				Socket socket = null;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x0a];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x14;
					buf[0x09] = (byte) resourcepacknamelength;
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfo.deserialize(is);
				} catch (IOException e) {
					this.amusic.logger.warn(e.getMessage());
				} finally {
					this.amusic.packetend(socket);
				}
				if(info == null) {
					return false;
				}
				resourcepacks.put(resourcepackname, info);
			}
			this.amusic.logger.info("AMusic resourcepack info cached update (" + resourcepackname + ")");
			return true;
		}
		
		public boolean removeResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfo> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfo oinfo = resourcepacks.get(resourcepackname);
				if(oinfo == null) {
					return false;
				}
				int i = resourcepacks.size();
				--i;
				if(i < 0) {
					i = 0;
				}
				HashMap<String, ResourcepackInfo> newresourcepacks = new HashMap<String, ResourcepackInfo>(i);
				String[] resourcepackinfolist = new String[i];
				Set<Entry<String, ResourcepackInfo>> entryset = newresourcepacks.entrySet();
				Iterator<Entry<String, ResourcepackInfo>> iterator = entryset.iterator();
				while(--i > -1 && iterator.hasNext()) {
					Entry<String, ResourcepackInfo> entry = iterator.next();
					String entrykey = entry.getKey();
					if(resourcepackname.equals(entrykey)) {
						continue;
					}
					entryset.add(entry);
					resourcepackinfolist[i] = entrykey;
				}
				
				this.resourcepacks = newresourcepacks;
				this.resourcepackinfolist = resourcepackinfolist;
			}
			this.amusic.logger.info("AMusic resourcepack info cached remove (" + resourcepackname + ")");
			return true;
		}
	}

}
