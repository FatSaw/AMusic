package me.bomb.amusic;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.net.SocketFactory;

import me.bomb.amusic.api.AMusic;
import me.bomb.amusic.api.LoadPackResult;
import me.bomb.amusic.api.RepeatType;
import me.bomb.amusic.api.ResourcepackInfo;
import me.bomb.amusic.resourcepack.ResourcepackInfoImpl;
import me.bomb.amusic.util.Logger;

public final class ClientAMusic implements AMusic {
	
	public final Logger logger;
	private final InetAddress hostip, remoteip;
	private final int port, timeout;
	private final long cachetimeoutfail, cachetimeoutsuccess;
	private final SocketFactory socketfactory;
	private final Executor executor;
	
	private ResourcepackInfoCache resourcepackinfocache;
	private ConcurrentHashMap<UUID, ResourcepackInfoImpl> cachePlayerResourcepackInfos = new ConcurrentHashMap<>();
	
	public ClientAMusic(Logger logger, InetAddress hostip, InetAddress remoteip, int port, int timeout, long cachetimeoutfail, long cachetimeoutsuccess, SocketFactory socketfactory, Executor executor) {
		this.logger = logger;
		this.hostip = hostip;
		this.remoteip = remoteip;
		this.port = port;
		this.timeout = timeout;
		this.cachetimeoutfail = cachetimeoutfail;
		this.cachetimeoutsuccess = cachetimeoutsuccess;
		this.socketfactory = socketfactory;
		this.executor = executor;
	}
	
	@Override
	public boolean updateCache() {
		this.resourcepackinfocache.interrupt();
		return true;
	}

	@Override
	public void enable() {
		ResourcepackInfoCache resourcepackinfocache = new ResourcepackInfoCache(this, this.cachetimeoutfail, this.cachetimeoutsuccess);
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
		cachePlayerResourcepackInfos.clear();
		this.logger.info("AMusic connect client stopped!");
	}
	
	@Override
	public void login(UUID playeruuid) {
		//TODO: Add uuid to track for cache synchronizations for uuid based get
	}
	
	@Override
	public void logout(UUID playeruuid) {
		cachePlayerResourcepackInfos.remove(playeruuid);
	}
	
	@Override
	public boolean loadResourcepack(UUID[] playeruuid, String name, boolean update, Consumer<LoadPackResult> resultConsumer) {
		if(name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x01; //PACKET ID
					byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
					int namelength = nameb.length;
					if(namelength > 0xFF) {
						namelength = 0xFF;
					}
					buf[0x09] = (byte) namelength;
					int targetcount = 0;
					if(playeruuid != null) {
						targetcount = playeruuid.length;
						if(targetcount > 0xFFFF) {
							targetcount = 0xFFFF;
						}
					}
					buf[0x0a] = (byte) targetcount;
					buf[0x0b] = (byte) (targetcount >>> 8);
					byte flags = 0;
					if(update) {
						flags |= 0x01;
					}
					final boolean hasconsumer = resultConsumer != null;
					if(hasconsumer) {
						flags |= 0x02;
					}
					buf[0x0c] = flags;
					os.write(buf, 0, 0x0D);
					os.write(nameb, 0, namelength);
					if(targetcount > 0) {
						int i = targetcount >> 4;
						++i;
						while(--i > -1) {
							int j = targetcount;
							if(j > 0x10) {
								j = 0x10;
							}
							int wbl = j << 4;
							int off = wbl;
							while(--j > -1) {
								UUID uuid = playeruuid[--targetcount];
								long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								msb>>>=8;
								buf[--off] = (byte) msb;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
								lsb>>>=8;
								buf[--off] = (byte) lsb;
							}
							os.write(buf, 0, wbl);
						}
					}
					if(hasconsumer) {
						InputStream is = socket.getInputStream();
						int off = 0;
						while(off < 1) {
							int n = is.read(buf, off, 1 - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						byte status = buf[0];
						switch (status) {
						case 1:
							resultConsumer.accept(LoadPackResult.DISPATCHED);
							if(playeruuid != null) {
								int uuidcount = playeruuid.length;
								while(--uuidcount > -1) {
									UUID uuid = playeruuid[uuidcount];
									cachePlayerResourcepackInfos.remove(uuid);
								}
							}
						break;
						case 2:
							resultConsumer.accept(LoadPackResult.NOTEXSIST);
						break;
						case 3:
							resultConsumer.accept(LoadPackResult.PACKED);
							if(update) {
								if(!resourcepackinfocache.updateResourcepackInfo(name)) {
									resourcepackinfocache.addResourcepackInfo(name);
								}
							}
						break;
						case 4:
							resultConsumer.accept(LoadPackResult.REMOVED);
							if(update) {
								resourcepackinfocache.removeResourcepackInfo(name);
							}
						break;
						case 5:
							resultConsumer.accept(LoadPackResult.UNAVILABLE);
						break;
						}
					} else {
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
								cachePlayerResourcepackInfos.remove(uuid);
							}
						}
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public boolean playSound(UUID playeruuid, String name, Consumer<Boolean> resultConsumer) {
		if(playeruuid == null || name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Boolean success = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x1A];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x02; //PACKET ID
					long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
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
					byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
					int length = nameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x19] = (byte) length;
					os.write(buf, 0, buf.length);
					os.write(nameb, 0, length);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 1) {
						int n = is.read(buf, off, 1 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					byte result = buf[0];
					success = result == 0 ? Boolean.FALSE : Boolean.TRUE;
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
	public boolean stopSound(UUID playeruuid, Consumer<Boolean> resultConsumer) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Boolean success = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x19];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x03; //PACKET ID
					long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
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
					os.write(buf, 0, buf.length);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 1) {
						int n = is.read(buf, off, 1 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					byte result = buf[0];
					success = result == 0 ? Boolean.FALSE : Boolean.TRUE;
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
	public boolean setRepeat(UUID playeruuid, RepeatType repeattype) {
		if(playeruuid == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x1A];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x04; //PACKET ID
					long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
					buf[0x09] = (byte) msb;
					msb>>>=8;
					buf[0x0A] = (byte) msb;
					msb>>>=8;
					buf[0x0B] = (byte) msb;
					msb>>>=8;
					buf[0x0C] = (byte) msb;
					msb>>>=8;
					buf[0x0D] = (byte) msb;
					msb>>>=8;
					buf[0x0E] = (byte) msb;
					msb>>>=8;
					buf[0x0F] = (byte) msb;
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
					buf[0X19] = repeattype == null ? (byte)0 : repeattype == RepeatType.PLAYALL ? (byte)1 : repeattype == RepeatType.RANDOM ? (byte)2 : repeattype == RepeatType.REPEATALL ? (byte)3 : repeattype == RepeatType.REPEATONE ? (byte)4 : (byte)0;
					os.write(buf, 0, buf.length);
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public final boolean setResourcepackCustomdata(String resourcepackname, byte[] customdata, Consumer<Boolean> resultConsumer) {
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
					buf[0x08] = 0x05; //PACKET ID
					buf[0x09] = (byte) resourcepacknamelength;
					buf[0x0a] = (byte) customdatalength;
					buf[0x0b] = (byte) (customdatalength >>> 8);
					os.write(buf, 0, buf.length);
					os.write(resourcepacknameb, 0, resourcepacknamelength);
					os.write(customdata, 0, customdatalength);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 1) {
						int n = is.read(buf, off, 1 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
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
	public boolean getLoadedPlayers(String playlistname, Consumer<UUID[]> resultConsumer) {
		if(playlistname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				UUID[] players = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0x100];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x06; //PACKET ID
					byte[] playlistnameb = playlistname.getBytes(StandardCharsets.UTF_8);
					int length = playlistnameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x09] = (byte) length;
					os.write(buf, 0, 0x0a);
					os.write(playlistnameb, 0, length);
					playlistnameb = null;
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int count = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
					
					if(count > 0) {
						players = new UUID[count];
						int i = count >> 4;
						++i;
						while(--i > -1) {
							int j = count;
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
								players[--count] = new UUID(msb, lsb);
							}
						}
					}
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(players);
			}
		};
		executor.execute(r);
		return true;
	}
	
	@Override
	public boolean getLoadedResourcepackName(UUID playeruuid, Consumer<String> resultConsumer) {
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
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x07; //PACKET ID
					
					long msb = playeruuid.getMostSignificantBits(), lsb = playeruuid.getLeastSignificantBits();
					buf[0x09] = (byte) msb;
					msb>>>=8;
					buf[0x0A] = (byte) msb;
					msb>>>=8;
					buf[0x0B] = (byte) msb;
					msb>>>=8;
					buf[0x0C] = (byte) msb;
					msb>>>=8;
					buf[0x0D] = (byte) msb;
					msb>>>=8;
					buf[0x0E] = (byte) msb;
					msb>>>=8;
					buf[0x0F] = (byte) msb;
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
					os.write(buf, 0, 0x19);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 1) {
						int n = is.read(buf, off, 1 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int length = 0xFF & buf[0];
					if(length > 0) {
						off = 0;
						while(off < length) {
							int n = is.read(buf, off, length - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
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
	
	
	@Override
	public String[] getResourcepackInfoListCached() {
		return this.resourcepackinfocache.resourcepackinfolist;
	}

	@Override
	public final boolean getResourcepackInfoList(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				String[] names = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x08; //PACKET ID
					os.write(buf, 0, 0x09);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					byte[] bufl = new byte[i];
					off = 0;
					while(off < bufl.length) {
						int n = is.read(bufl, off, bufl.length - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					names = new String[i];
					while(--i > -1) {
						int len = bufl[i] & 0xFF;
						off = 0;
						while(off < len) {
							int n = is.read(buf, off, len - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						names[i] = new String(buf, 0, len, StandardCharsets.UTF_8);
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
	public ResourcepackInfoImpl getResourcepackInfoCached(String resourcepackname) {
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
				ResourcepackInfoImpl info = null;
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
					buf[0x08] = 0x09; //PACKET ID
					byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
					int length = resourcepacknameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x09] = (byte) length;
					os.write(buf, 0, buf.length);
					buf = null;
					os.write(resourcepacknameb, 0, length);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfoImpl.deserialize(is);
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
	public ResourcepackInfo getResourcepackInfoCached(UUID playeruuid) {
		if(playeruuid == null) {
			return null;
		}
		return cachePlayerResourcepackInfos.get(playeruuid);
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
				buf[0x08] = 0x0A; //PACKET ID
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
				ResourcepackInfoImpl info = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					os.write(buf, 0, buf.length);
					buf = null;
					InputStream is = socket.getInputStream();
					info = ResourcepackInfoImpl.deserialize(is);
				} catch (IOException e) {
				} finally {
					ClientAMusic.this.packetend(socket);
				}
				resultConsumer.accept(info);
				cachePlayerResourcepackInfos.put(playeruuid, info);
			}
		};
		executor.execute(r);
		return true;
	}
	
	
	@Override
	public String[] getSourceResourcepackNameListCached() {
		return this.resourcepackinfocache.resourcepacklist;
	}
	
	@Override
	public final boolean getSourceResourcepackNameList(Consumer<String[]> resultConsumer) {
		Runnable r = new Runnable() {
			public void run() {
				Socket socket = null;
				String[] names = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x0B; //PACKET ID
					os.write(buf, 0, 0x09);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					byte[] bufl = new byte[i];
					off = 0;
					while(off < bufl.length) {
						int n = is.read(bufl, off, bufl.length - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					names = new String[i];
					while(--i > -1) {
						int len = bufl[i] & 0xFF;
						off = 0;
						while(off < len) {
							int n = is.read(buf, off, len - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						names[i] = new String(buf, 0, len, StandardCharsets.UTF_8);
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
	public String[] getSourceSoundnameListCached(String resourcepackname) {
		return this.resourcepackinfocache.resourcepacksoundnames.get(resourcepackname);
	}
	
	@Override
	public boolean getSourceSoundnameList(String resourcepackname, Consumer<String[]> resultConsumer) {
		if(resourcepackname == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				String[] soundnames = null;
				Socket socket = null;
				try {
					socket = ClientAMusic.this.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x0C; //PACKET ID
					byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
					int length = resourcepacknameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x09] = (byte) length;
					os.write(buf, 0, 0x0a);
					os.write(resourcepacknameb, 0, length);
					resourcepacknameb = null;
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					
					byte[] bufl = new byte[i];
					off = 0;
					while(off < bufl.length) {
						int n = is.read(bufl, off, bufl.length - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					soundnames = new String[i];
					while(--i > -1) {
						int len = bufl[i] & 0xFF;
						off = 0;
						while(off < len) {
							int n = is.read(buf, off, len - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						soundnames[i] = new String(buf, 0, len, StandardCharsets.UTF_8);
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
		private final long synchronizationdelayfailed, synchronizationdelaysuccess;
		private final HashMap<String, ResourcepackInfoImpl> defaultresourcepacksmap = new HashMap<String, ResourcepackInfoImpl>(0);
		private final HashMap<String, String[]> defaultsoundnamesmap = new HashMap<String, String[]>(0);
		protected String[] resourcepackinfolist = null, resourcepacklist = null;
		protected HashMap<String, ResourcepackInfoImpl> resourcepacks = this.defaultresourcepacksmap;
		protected HashMap<String, String[]> resourcepacksoundnames = this.defaultsoundnamesmap;
		protected volatile boolean run;
		
		private ResourcepackInfoCache(ClientAMusic amusic, long synchronizationdelayfailed, long synchronizationdelaysuccess) {
			this.amusic = amusic;
			this.synchronizationdelayfailed = synchronizationdelayfailed;
			this.synchronizationdelaysuccess = synchronizationdelaysuccess;
		}
		
		@Override
		public void run() {
			byte[] checkcacheupdatepacket = new byte[0x19];
			try {
				SecureRandom sr = SecureRandom.getInstanceStrong();
				sr.nextBytes(checkcacheupdatepacket);
			} catch (NoSuchAlgorithmException e) {
				Random r = new Random();
				r.nextBytes(checkcacheupdatepacket);
			}
			checkcacheupdatepacket[0x00] = 'a';
			checkcacheupdatepacket[0x01] = 'm';
			checkcacheupdatepacket[0x02] = 'r';
			checkcacheupdatepacket[0x03] = 'a';
			checkcacheupdatepacket[0x04] = 0x00;
			checkcacheupdatepacket[0x05] = 0x00;
			checkcacheupdatepacket[0x06] = 0x00;
			checkcacheupdatepacket[0x07] = 0x00;
			checkcacheupdatepacket[0x08] = 0x00; //PACKET ID
			String synchronizationfailedmsg = "Resourcepack info synchronization: FAIL\nNext synchronization through: ".concat(Long.toString(this.synchronizationdelayfailed)).concat(" milliseconds");
			//String synchronizationnotneedmsg = "Resourcepack info synchronization: NOT NEED\nNext synchronization through: ".concat(Long.toString(this.synchronizationdelaysuccess)).concat(" milliseconds");
			String synchronizationsuccessmsg = "Resourcepack info synchronization: SUCCESS\nNext synchronization through: ".concat(Long.toString(this.synchronizationdelaysuccess)).concat(" milliseconds");
			boolean cacheupdated = false;
			while(this.run) {
				Socket socket = null;
				boolean requestsuccess = false, needcacheupdate = false;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					os.write(checkcacheupdatepacket, 0, 0x19);
					InputStream is = socket.getInputStream();
					int flags = is.read();
					if(flags == -1) {
						throw new EOFException();
					}
					needcacheupdate = (flags & 0x01) == 0x01;
					requestsuccess = true;
				} catch (IOException e) {
				} finally {
					this.amusic.packetend(socket);
				}
				
				if(!requestsuccess) {
					this.amusic.logger.warn(synchronizationfailedmsg);
					try {
						Thread.sleep(this.synchronizationdelayfailed);
					} catch (InterruptedException e2) {
						Thread.interrupted();
					}
					continue;
				}
				
				if(!needcacheupdate) {
					if(cacheupdated) {
						this.amusic.logger.info(synchronizationsuccessmsg);
					}
					//this.amusic.logger.info(cacheupdated ? synchronizationsuccessmsg : synchronizationnotneedmsg);
					cacheupdated = false;
					try {
						Thread.sleep(this.synchronizationdelaysuccess);
					} catch (InterruptedException e) {
						Thread.interrupted();
					}
					continue;
				}
				
				byte[] lengths = null;
				byte[][] namesbytes = null;
				try {
					socket = this.amusic.socket();
					OutputStream os = socket.getOutputStream();
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x08; //PACKET ID
					os.write(buf, 0, 0x09);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					int i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					lengths = new byte[i];
					off = 0;
					while(off < lengths.length) {
						int n = is.read(lengths, off, lengths.length - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					namesbytes = new byte[i][];
					while(--i > -1) {
						int len = lengths[i] & 0xFF;
						byte[] strbuf = new byte[len];
						off = 0;
						while(off < len) {
							int n = is.read(strbuf, off, len - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						namesbytes[i] = strbuf;
					}
				} catch (IOException e) {
				} finally {
					this.amusic.packetend(socket);
				}
				if(lengths == null || namesbytes == null) {
					continue;
				}
				int i = namesbytes.length, j = 0;
				String[] resourcepackinfolist = new String[i];
				final HashMap<String, ResourcepackInfoImpl> resourcepacks = new HashMap<String, ResourcepackInfoImpl>(i);
				while(--i > -1) {
					byte[] resourcepacknameb = namesbytes[i];
					byte len = lengths[i];
					ResourcepackInfoImpl info = null;
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
						buf[0x08] = 0x09; //PACKET ID
						buf[0x09] = len;
						os.write(buf, 0, buf.length);
						os.write(resourcepacknameb, 0, len & 0xFF);
						InputStream is = socket.getInputStream();
						info = ResourcepackInfoImpl.deserialize(is);
						++j;
					} catch (IOException e) {
					} finally {
						this.amusic.packetend(socket);
					}
					if(info != null) {
						String resourcepackname = info.getPackname();
						resourcepackinfolist[i] = resourcepackname;
						resourcepacks.put(resourcepackname, info);
						//this.amusic.logger.info("Resourcepack info synchronized (" + resourcepackname + ")");
					}
				}
				i = namesbytes.length;
				this.resourcepackinfolist = resourcepackinfolist;
				this.resourcepacks = resourcepacks;
				this.amusic.logger.info("Resourcepack info synchronized (" + Integer.toString(j) + "/" + Integer.toString(i) + ")");
				
				this.synchronizeDirs();
				cacheupdated = true;
			}
			this.resourcepackinfolist = null;
			this.resourcepacks = this.defaultresourcepacksmap;
			this.resourcepacklist = null;
			this.resourcepacksoundnames = this.defaultsoundnamesmap;
		}
		
		private void synchronizeDirs() {
			Socket socket = null;
			int i = 0, j = 0;
			byte[] lengths = null;
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
				buf[0x08] = 0x0B; //PACKET ID
				os.write(buf, 0, buf.length);
				InputStream is = socket.getInputStream();
				int off = 0;
				while(off < 2) {
					int n = is.read(buf, off, 2 - off);
					if (n == -1) {
				        throw new EOFException();
				    }
					off += n;
				}
				i = 0xFF & buf[0] | 0xFF & buf[1] << 8;
				lengths = new byte[i];
				off = 0;
				while(off < lengths.length) {
					int n = is.read(lengths, off, lengths.length - off);
					if (n == -1) {
				        throw new EOFException();
				    }
					off += n;
				}
				resourcepacklistbytes = new byte[i][];
				resourcepacklist = new String[i];
				while(--i > -1) {
					int len = lengths[i] & 0xFF;
					byte[] strbuf = new byte[len];
					off = 0;
					while(off < len) {
						int n = is.read(strbuf, off, len - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
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
					byte[] buf = new byte[0xFF];
					buf[0x00] = 'a';
					buf[0x01] = 'm';
					buf[0x02] = 'r';
					buf[0x03] = 'a';
					buf[0x04] = 0x00;
					buf[0x05] = 0x00;
					buf[0x06] = 0x00;
					buf[0x07] = 0x00;
					buf[0x08] = 0x0C; //PACKET ID
					buf[0x09] = len;
					os.write(buf, 0, 0x0A);
					os.write(resourcepacknameb, 0, len & 0xFF);
					InputStream is = socket.getInputStream();
					int off = 0;
					while(off < 2) {
						int n = is.read(buf, off, 2 - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					j = 0xFF & buf[0] | 0xFF & buf[1] << 8;
					byte[] bufl = new byte[j];
					off = 0;
					while(off < j) {
						int n = is.read(bufl, off, j - off);
						if (n == -1) {
					        throw new EOFException();
					    }
						off += n;
					}
					soundnames = new String[j];
					while(--j > -1) {
						int slen = bufl[j] & 0xFF;
						off = 0;
						while(off < slen) {
							int n = is.read(buf, off, slen - off);
							if (n == -1) {
						        throw new EOFException();
						    }
							off += n;
						}
						soundnames[j] = new String(buf, 0, slen, StandardCharsets.UTF_8);
					}
				} catch (IOException e) {
				} finally {
					this.amusic.packetend(socket);
				}
				resourcepacksoundnames.put(resourcepacklist[i], soundnames);
			}
			
			this.resourcepacksoundnames = resourcepacksoundnames;
			this.amusic.logger.info("Music directory list synchronized (" + Integer.toString(resourcepacklist.length) + ")");
		}
		
		public boolean addResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfoImpl> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfoImpl oinfo = resourcepacks.get(resourcepackname);
				if(oinfo != null) {
					return false;
				}
				ResourcepackInfoImpl info = null;
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
					buf[0x08] = 0x09; //PACKET ID
					byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
					int length = resourcepacknameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x09] = (byte) length;
					os.write(buf, 0, buf.length);
					os.write(resourcepacknameb, 0, length);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfoImpl.deserialize(is);
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
				Set<Entry<String, ResourcepackInfoImpl>> entryset = resourcepacks.entrySet();
				HashMap<String, ResourcepackInfoImpl> newresourcepacks = new HashMap<String, ResourcepackInfoImpl>(i);
				String[] resourcepackinfolist = new String[i];
				Iterator<Entry<String, ResourcepackInfoImpl>> iterator = entryset.iterator();
				while(--i > 0 && iterator.hasNext()) {
					Entry<String, ResourcepackInfoImpl> entry = iterator.next();
					String entrykey = entry.getKey();
					newresourcepacks.put(entrykey, entry.getValue());
					resourcepackinfolist[i] = entrykey;
				}
				newresourcepacks.put(resourcepackname, info);
				resourcepackinfolist[i] = resourcepackname;

				this.resourcepacks = newresourcepacks;
				this.resourcepackinfolist = resourcepackinfolist;
			}
			//this.amusic.logger.info("AMusic resourcepack info cached add (" + resourcepackname + ")");
			this.synchronizeDirs();
			return true;
		}
		
		public boolean updateResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfoImpl> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfoImpl oinfo = resourcepacks.get(resourcepackname);
				if(oinfo == null) {
					return false;
				}
				ResourcepackInfoImpl info = null;
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
					buf[0x08] = 0x09; //PACKET ID
					byte[] resourcepacknameb = resourcepackname.getBytes(StandardCharsets.UTF_8);
					int length = resourcepacknameb.length;
					if(length > 0xFF) {
						length = 0xFF;
					}
					buf[0x09] = (byte) length;
					os.write(buf, 0, buf.length);
					os.write(resourcepacknameb, 0, length);
					InputStream is = socket.getInputStream();
					info = ResourcepackInfoImpl.deserialize(is);
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
			//this.amusic.logger.info("AMusic resourcepack info cached update (" + resourcepackname + ")");
			return true;
		}
		
		public boolean removeResourcepackInfo(String resourcepackname) {
			HashMap<String, ResourcepackInfoImpl> resourcepacks = this.resourcepacks;
			synchronized(resourcepacks) {
				ResourcepackInfoImpl oinfo = resourcepacks.get(resourcepackname);
				if(oinfo == null) {
					return false;
				}
				int i = resourcepacks.size();
				--i;
				if(i < 0) {
					i = 0;
				}
				Set<Entry<String, ResourcepackInfoImpl>> entryset = resourcepacks.entrySet();
				HashMap<String, ResourcepackInfoImpl> newresourcepacks = new HashMap<String, ResourcepackInfoImpl>(i);
				String[] resourcepackinfolist = new String[i];
				Iterator<Entry<String, ResourcepackInfoImpl>> iterator = entryset.iterator();
				while(--i > -1 && iterator.hasNext()) {
					Entry<String, ResourcepackInfoImpl> entry = iterator.next();
					String entrykey = entry.getKey();
					if(resourcepackname.equals(entrykey)) {
						continue;
					}
					newresourcepacks.put(entrykey, entry.getValue());
					resourcepackinfolist[i] = entrykey;
				}
				
				this.resourcepacks = newresourcepacks;
				this.resourcepackinfolist = resourcepackinfolist;
			}
			//this.amusic.logger.info("AMusic resourcepack info cached remove (" + resourcepackname + ")");
			this.synchronizeDirs();
			return true;
		}
	}

}
