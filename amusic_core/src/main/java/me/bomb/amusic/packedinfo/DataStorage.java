package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map.Entry;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;

import static me.bomb.amusic.util.Base64Utils.toBase64Url;
import static me.bomb.amusic.util.NameFilter.filterName;
import static me.bomb.amusic.util.Base64Utils.fromBase64Url;

final class DataStorage extends me.bomb.amusic.packedinfo.Data implements Runnable {
	
	private static final String FORMAT = ".ampi";
	private static final byte FORMATSIZE = 5;
	private static final byte VERSION = 2;
	private static final DirectoryStream.Filter<Path> ampifilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			final String name = path.getFileName().toString();
			return name.startsWith(FORMAT, name.length() - FORMATSIZE);
		}
    };
	
    private final FileSystemProvider fs;
    private final Path datadirectory;
	private final Thread savethread;
	private volatile boolean run;
	
	protected DataStorage(Path datadirectory, boolean lockwrite) {
		super(lockwrite);
		this.datadirectory = datadirectory;
		this.run = !this.lockwrite;
		this.savethread = this.lockwrite ? null : new Thread(this);
		this.fs = datadirectory.getFileSystem().provider();
	}

	@Override
	protected void save() {
		if(this.lockwrite) {
			return;
		}
		synchronized(this) {
			notify();
		}
	}

	@Override
	public void load() {
		options.clear();
		DirectoryStream<Path> ds = null;
		try {
			ds = fs.newDirectoryStream(datadirectory, ampifilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path ampifile = it.next();
				String id = ampifile.getFileName().toString();
				final int namesize = id.length() - FORMATSIZE;
				final Path datapath;
				try {
					id = id.substring(0, namesize);
					datapath = datadirectory.resolve(id.concat(".zip"));
					id = fromBase64Url(id);
				} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
					continue;
				}
				InputStream is = null;
				try {
					is = fs.newInputStream(ampifile);
					byte[] buf = new byte[8];
					if(is.read(buf) != 8 || buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'p' || buf[3] != 'i' || buf[4] != 0 || buf[7] != 0) {
						is.close();
						continue;
					}
					byte version = (byte) is.read();
					buf = new byte[4];
					byte[] sha1 = new byte[20];
					int packednamelength;
					is.read(buf);
					is.read(sha1);
					if(version != VERSION || (packednamelength = is.read()) == -1) {
						is.close();
						continue;
					}
					int packedsize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
					buf = new byte[packednamelength];
					is.read(buf);
					String packedname = new String(buf, StandardCharsets.UTF_8);
					buf = new byte[2];
					if(is.read(buf) != 2) {
						is.close();
						continue;
					}
					int soundcount = 0x0000FFFF;
					soundcount &= 0xFF & buf[0] | buf[1] << 8;
					byte[] namelengths = new byte[soundcount];
					buf = new byte[soundcount<<1];
					is.read(namelengths);
					short[] lengths = new short[soundcount];
					is.read(buf);
					for(int k = 0,j = 0; k < soundcount; ++k, ++j) {
						lengths[k] = (short) (buf[j] & 0xFF | buf[++j]<<8);
					}
					soundcount = (short) lengths.length;
					SoundInfo[] sounds = new SoundInfo[soundcount];
					int j = 0;
					while(j < soundcount) {
						buf = new byte[0xFF & namelengths[j]];
						is.read(buf);
						sounds[j] = new SoundInfo(new String(buf, StandardCharsets.UTF_8), lengths[j]);
						++j;
					}
					is.close();
					DefaultDataEntry dataentry = new DefaultDataEntry(datapath, packedsize, packedname, sounds, sha1);
					dataentry.saved = true;
					options.put(id, dataentry);
				} catch (IOException e1) {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e2) {
						}
					}
					continue;
				}
			}
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				try {
					ds.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	@Override
	public void run() {
		while(run) {
			synchronized(this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
			DirectoryStream<Path> ds = null;
			try {
				ds = fs.newDirectoryStream(datadirectory, ampifilter);
				final Iterator<Path> it = ds.iterator();
				while(it.hasNext()) {
					final Path ampifile = it.next();
					String id = ampifile.getFileName().toString();
					final int namesize = id.length() - FORMATSIZE;
					try {
						id = id.substring(0, namesize);
						id = fromBase64Url(id);
					} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
						fs.deleteIfExists(ampifile);
						continue;
					}
					if(options.containsKey(id)) {
						continue;
					}
					fs.deleteIfExists(ampifile);
				}
			} catch (IOException e) {
			} finally {
				if (ds != null) {
					try {
						ds.close();
					} catch (IOException e) {
					}
				}
			}
			
			for(Entry<String, DataEntry> entry : options.entrySet()) {
				String id = entry.getKey();
				id = toBase64Url(id);
				DefaultDataEntry dataentry = (DefaultDataEntry) entry.getValue();
				if(dataentry.saved || dataentry.size < 0 || dataentry.name == null || dataentry.sounds == null || dataentry.sha1 == null || dataentry.sha1.length != 20) {
					continue;
				}
				Path ampifile = datadirectory.resolve(id.concat(FORMAT));
				OutputStream os = null;
				try {
					int soundcount = dataentry.sounds.length;
					os = fs.newOutputStream(ampifile);
					os.write('a'); //FORMATID
					os.write('m'); //FORMATID
					os.write('p'); //FORMATID
					os.write('i'); //FORMATID
					os.write(0); //FORMATID
					os.write(0); //0
					os.write(0); //0
					os.write(0); //FORMATID
					os.write(VERSION); //VERSION
					int entryfilesize = dataentry.size;
					//fos.write(dataentry.size);
					os.write((byte)entryfilesize); //FILESIZE
					entryfilesize>>=8;
					os.write((byte)entryfilesize); //FILESIZE
					entryfilesize>>=8;
					os.write((byte)entryfilesize); //FILESIZE
					entryfilesize>>=8;
					os.write((byte)entryfilesize); //FILESIZE
					os.write(dataentry.sha1); //SHA1
					byte[] packednamebytes = dataentry.name.getBytes(StandardCharsets.UTF_8);
					int packednamelength = packednamebytes.length;
					if(packednamelength > 0xFF) {
						packednamelength = 0xFF;
						byte[] npackednamebytes = new byte[0xFF];
						System.arraycopy(packednamebytes, 0, npackednamebytes, 0, packednamelength);
						packednamebytes = npackednamebytes;
					}
					os.write((byte) packednamelength); //PACKED FILE PATH
					os.write(packednamebytes); //PACKED FILE PATH
					if(soundcount > 0x0000FFFF) {
						soundcount = 0x0000FFFF;
					}
					byte[] soundcountb = new byte[2];
					soundcountb[0] = (byte) soundcount;
					soundcountb[1] = (byte) (soundcount>>8);
					os.write(soundcountb);
					int lengthscount = soundcount<<1;
					byte[] namelengths = new byte[soundcount], lengths = new byte[lengthscount];
					short i = 0, j = 0;
					int totalsoundnamelength = 0;
					byte[][] anames = new byte[soundcount][];
					while(i < soundcount) {
						byte[] soundnamebytes = dataentry.sounds[i].name.getBytes(StandardCharsets.UTF_8);
						int soundnamelength = soundnamebytes.length;
						if(soundnamelength > 0xFF) {
							soundnamelength = 0xFF;
							byte[] nsoundnamebytes = new byte[0xFF];
							System.arraycopy(soundnamebytes, 0, nsoundnamebytes, 0, soundnamelength);
							soundnamebytes = nsoundnamebytes;
						}
						totalsoundnamelength += soundnamelength;
						anames[i] = soundnamebytes;
						namelengths[i] = (byte) soundnamelength;
						short length = dataentry.sounds[i].length;
						++i;
						lengths[j] = (byte) length;
						length >>= 8;
						++j;
						lengths[j] = (byte) length;
						++j;
					}
					byte[] names = new byte[totalsoundnamelength];
					int namesi = 0;
					i = 0;
					while(i < soundcount) {
						byte[] soundnamebytes = anames[i];
						int soundnamelength = soundnamebytes.length;
						System.arraycopy(soundnamebytes, 0, names, namesi, soundnamelength);
						namesi+=soundnamelength;
						++i;
					}
					os.write(namelengths); //NAME LENGTHS  ENTRY 0-255
					os.write(lengths); //SOUND LENGTHS  ENTRY 0-65535
					os.write(names); //SOUND LENGTHS ALL 0-8355585 32767*255
					
					os.close();
					dataentry.saved = true;
				} catch (IOException e1) {
					if(os != null) {
						try {
							os.close();
						} catch (IOException e2) {
						}
					}
				}
			}
		}
	}
	
	public void start() {
		savethread.start();
	}
	
	public void end() {
		run = false;
		synchronized(this) {
			this.notify();
		}
	}
	
	public ResourcePacker createPacker(final String id, final SoundSource soundsource, final PackSource packsource) {
		if(this.lockwrite || id == null || soundsource == null || !soundsource.exists(id)) {
			return null;
		}
		final String filteredid = filterName(id);
		ResourcePacker packer = new ResourcePacker(soundsource, filteredid, packsource);
		return packer;
	}
	
	public boolean update(final String id, final ResourcePacker packer) {
		if(this.lockwrite || id == null) {
			return false;
		}
		if(packer == null) {
			final boolean deleted;
			DefaultDataEntry data = (DefaultDataEntry) options.remove(id);
			if(data == null) {
				return false;
			}
			try {
				deleted = fs.deleteIfExists(data.datapath);
			} catch (IOException e) {
				return false;
			}
			save();
			return deleted;
		}
		packer.run();
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return false;
		}
		final Path resourcefile = datadirectory.resolve(toBase64Url(id).concat(".zip"));
		OutputStream os = null;
		try {
			os = fs.newOutputStream(resourcefile);
			os.write(resourcepack);
			os.close();
		} catch (IOException e1) {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e2) {
				}
			}
		}
		
		DefaultDataEntry data = (DefaultDataEntry) getPlaylist(id);
		if (data != null) {
			data.size = resourcepack.length;
			data.sounds = packer.sounds;
			data.sha1 = packer.sha1;
			data.saved = false;
			save();
			return true;
		}
		
		options.put(id, new DefaultDataEntry(resourcefile, resourcepack.length, filterName(id), packer.sounds, packer.sha1));
		save();
		return true;
	}

}
