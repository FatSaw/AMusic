package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.UUID;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;

import static me.bomb.amusic.util.NameFilter.filterName;

final class DataStorage extends me.bomb.amusic.packedinfo.Data {
	
	private static final String FORMAT = ".ampi";
	private static final byte FORMATSIZE = 5;
	private static final byte VERSION = 4;
	private static final DirectoryStream.Filter<Path> ampifilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			final String name = path.getFileName().toString();
			return name.startsWith(FORMAT, name.length() - FORMATSIZE);
		}
    };
	
    private final FileSystemProvider fs;
    private final Path datadirectory;
	
	protected DataStorage(Path datadirectory, boolean lockwrite) {
		super(lockwrite);
		this.datadirectory = datadirectory;
		this.fs = datadirectory.getFileSystem().provider();
	}

	/**
	 * Ignored.
	 */
	@Override
	protected void save() {
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
				id = id.substring(0, id.length() - FORMATSIZE);
				InputStream is = null;
				try {
					int skip = 36;
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
					skip+=packednamelength;
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
					skip+=soundcount<<2;
					byte[] namelengths = new byte[soundcount], splits = new byte[soundcount];
					buf = new byte[soundcount<<1];
					is.read(namelengths);
					is.read(splits);
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
						skip+=buf.length;
						sounds[j] = new SoundInfo(new String(buf, StandardCharsets.UTF_8), lengths[j], splits[j]);
						++j;
					}
					is.close();
					DefaultDataEntry dataentry = new DefaultDataEntry(skip, ampifile, id, packedsize, packedname, sounds, sha1);
					dataentry.saved = true;
					options.put(packedname, dataentry);
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
	
	private void saveAmp(String id, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] resource) {
		if(size < 0 || name == null || sounds == null || sha1 == null || sha1.length != 20) {
			return;
		}
		int skip = 36;
		Path ampifile = datadirectory.resolve(id.concat(FORMAT));
		OutputStream os = null;
		try {
			int soundcount = sounds.length;
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
			int entryfilesize = size;
			//fos.write(dataentry.size);
			os.write((byte)entryfilesize); //FILESIZE
			entryfilesize>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			entryfilesize>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			entryfilesize>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			os.write(sha1); //SHA1
			byte[] packednamebytes = name.getBytes(StandardCharsets.UTF_8);
			int packednamelength = packednamebytes.length;
			if(packednamelength > 0xFF) {
				packednamelength = 0xFF;
				byte[] npackednamebytes = new byte[0xFF];
				System.arraycopy(packednamebytes, 0, npackednamebytes, 0, packednamelength);
				packednamebytes = npackednamebytes;
			}
			os.write((byte) packednamelength); //PACKED FILE PATH
			skip += packednamelength;
			os.write(packednamebytes); //PACKED FILE PATH
			if(soundcount > 0x0000FFFF) {
				soundcount = 0x0000FFFF;
			}
			byte[] soundcountb = new byte[2];
			soundcountb[0] = (byte) soundcount;
			soundcountb[1] = (byte) (soundcount>>8);
			os.write(soundcountb);
			int lengthscount = soundcount<<1;
			skip += soundcount<<2;
			byte[] namelengths = new byte[soundcount], splits = new byte[soundcount],lengths = new byte[lengthscount];
			short i = 0, j = 0;
			int totalsoundnamelength = 0;
			byte[][] anames = new byte[soundcount][];
			while(i < soundcount) {
				byte[] soundnamebytes = sounds[i].name.getBytes(StandardCharsets.UTF_8);
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
				splits[i] = sounds[i].split;
				short length = sounds[i].length;
				++i;
				lengths[j] = (byte) length;
				length >>= 8;
				++j;
				lengths[j] = (byte) length;
				++j;
			}
			skip += totalsoundnamelength;
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
			os.write(namelengths); //NAME LENGTHS ENTRY 0-255
			os.write(splits); //SOUND SPLITS ENTRY 0-255
			os.write(lengths); //SOUND LENGTHS ENTRY 0-65535
			os.write(names); //SOUND LENGTHS ALL 0-8355585 32767*255
			os.write(resource); //RESOURCEPACK ARCHIVE
			os.close();
		} catch (IOException e1) {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e2) {
				}
			}
		}
		DefaultDataEntry entry = new DefaultDataEntry(skip, ampifile, id, size, name, sounds, sha1);
		entry.saved = true;
		options.put(name, entry);
	}
	
	public ResourcePacker createPacker(final String id, final SoundSource soundsource, final PackSource packsource) {
		if(this.lockwrite || id == null || soundsource == null || !soundsource.exists(id)) {
			return null;
		}
		final String filteredid = filterName(id);
		ResourcePacker packer = new ResourcePacker(soundsource, filteredid, packsource);
		return packer;
	}
	
	public boolean update(final String name, final ResourcePacker packer) {
		if(this.lockwrite || name == null) {
			return false;
		}
		if(packer == null) {
			final boolean deleted;
			DefaultDataEntry data = (DefaultDataEntry) options.remove(name);
			if(data == null) {
				return false;
			}
			try {
				deleted = fs.deleteIfExists(data.datapath);
			} catch (IOException e) {
				return false;
			}
			return deleted;
		}
		packer.run();
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return false;
		}
		DataEntry entry = options.remove(name);
		String id = entry == null ? UUID.randomUUID().toString() : entry.storeid;
		saveAmp(id, resourcepack.length, name, packer.sounds, packer.sha1, resourcepack);
		return true;
	}
	
	/**
	 * Ignored.
	 */
	@Override
	public void start() {
	}
	
	/**
	 * Ignored.
	 */
	@Override
	public void end() {
	}

}
