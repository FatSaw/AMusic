package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.util.AMusicLogger;

import static me.bomb.amusic.util.NameFilter.filterName;

final class DataStorage extends me.bomb.amusic.packedinfo.Data {
	
	private static final String FORMAT = ".ampi";
	private static final byte FORMATSIZE = 5;
	private static final byte VERSION = 6;
	private static final DirectoryStream.Filter<Path> ampifilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			final String name = path.getFileName().toString();
			return name.startsWith(FORMAT, name.length() - FORMATSIZE);
		}
    };
	
    private final FileSystemProvider fs;
    private final Path datadirectory;
	
	protected DataStorage(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram, Path datadirectory) {
		super(soundsource, packsource, lockwrite, storeinram);
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
				String storeid = ampifile.getFileName().toString();
				storeid = storeid.substring(0, storeid.length() - FORMATSIZE);
				DataEntry entry = loadAmp(storeid);
				if(entry == null) {
					continue;
				}
				options.put(entry.name, entry);
				AMusicLogger.info("Pack \"".concat(storeid).concat("\" load success"));
			}
			AMusicLogger.info("Loaded ".concat(Integer.toString(options.size())).concat(" resourcepacks"));
			this.printRamUsage();
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
	
	private DataEntry loadAmp(String storeid) {
		if(storeid == null) {
			AMusicLogger.warn("Pack load fail (invalid values)");
			return null;
		}
		Path ampifile;
		try {
			ampifile = datadirectory.resolve(storeid.concat(FORMAT));
		} catch (InvalidPathException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid path)"));
			return null;
		}
		InputStream is = null;
		try {
			int skip = 100;
			is = fs.newInputStream(ampifile);
			byte[] buf = new byte[8];
			if(is.read(buf) != 8 || buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'p' || buf[3] != 'i' || buf[4] != 0 || buf[7] != 0) {
				is.close();
				AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid header)"));
				return null;
			}
			byte version = (byte) is.read();
			buf = new byte[4];
			byte[] sha1 = new byte[20];
			byte[] sha256 = new byte[0x20];
			byte[] buuids = new byte[0x20];
			int packednamelength;
			is.read(buf);
			is.read(sha1);
			is.read(sha256);
			is.read(buuids);
			if(version != VERSION || (packednamelength = is.read()) == -1) {
				is.close();
				AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid version)"));
				return null;
			}
			skip+=packednamelength;
			int packedsize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
			UUID bhea, bres;
			{
				int j = buuids.length;
				long msb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56, lsb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
				bhea = new UUID(msb, lsb);
				msb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
				lsb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
				bres = new UUID(msb, lsb);
			}
			buf = new byte[packednamelength];
			is.read(buf);
			String packedname = new String(buf, StandardCharsets.UTF_8);
			buf = new byte[2];
			is.read(buf);
			int soundcount = 0x0000FFFF;
			soundcount &= 0xFF & buf[0] | buf[1] << 8;
			skip+=soundcount<<4; //SOUND HASH
			skip+=soundcount<<2;
			buf = new byte[soundcount<<4];
			is.read(buf);
			UUID[] soundhashs = new UUID[soundcount];
			int i = soundcount, j = soundcount << 4;
			//--j;
			while(--i > -1) {
				long msb = (buf[--j] & 0xFFL) | (buf[--j] & 0xFFL) << 8 | (buf[--j] & 0xFFL) << 16 | (buf[--j] & 0xFFL) << 24 | (buf[--j] & 0xFFL) << 32 | (buf[--j] & 0xFFL) << 40 | (buf[--j] & 0xFFL) << 48 | (buf[--j] & 0xFFL) << 56, lsb = (buf[--j] & 0xFFL) | (buf[--j] & 0xFFL) << 8 | (buf[--j] & 0xFFL) << 16 | (buf[--j] & 0xFFL) << 24 | (buf[--j] & 0xFFL) << 32 | (buf[--j] & 0xFFL) << 40 | (buf[--j] & 0xFFL) << 48 | (buf[--j] & 0xFFL) << 56;
				soundhashs[i] = new UUID(msb, lsb);
			}
			byte[] namelengths = new byte[soundcount], splits = new byte[soundcount];
			buf = new byte[soundcount<<1];
			is.read(namelengths);
			is.read(splits);
			short[] lengths = new short[soundcount];
			is.read(buf);
			i = soundcount;
			j = soundcount<<1;
			while(--i > -1) {
				lengths[i] = (short) (buf[--j] & 0xFF | buf[--j]<<8);
			}
			soundcount = (short) lengths.length;
			SoundInfo[] sounds = new SoundInfo[soundcount];
			i = soundcount;
			while(--i > -1) {
				buf = new byte[0xFF & namelengths[i]];
				is.read(buf);
				skip+=buf.length;
				sounds[i] = new SoundInfo(new String(buf, StandardCharsets.UTF_8), soundhashs[i], lengths[i], splits[i]);
			}
			if(this.storeinram) {
				buf = new byte[packedsize];
				is.read(buf, 0, buf.length);
				RamDataEntry dataentry = new RamDataEntry(storeid, packedsize, packedname, sounds, sha1, sha256, bhea, bres, buf);
				is.close();
				final MessageDigest sha1hash;
				try {
					sha1hash = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					AMusicLogger.warn("Pack load fail (can not initialize SHA-1)");
					return null;
				}
				byte[] filesha1 = sha1hash.digest(buf);
				if(Arrays.equals(filesha1, sha1)) {
					return dataentry;
				} else {
					AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid checksum)"));
					return null;
				}
			}
			is.close();
			DefaultDataEntry dataentry = new DefaultDataEntry(skip, ampifile, storeid, packedsize, packedname, sounds, sha1, sha256, bhea, bres);
			return dataentry;
		} catch (IOException e1) {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (IO exception)"));
			AMusicLogger.error(e1.getMessage());
			return null;
		}
	}
	
	private DataEntry saveAmp(String storeid, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] sha256, UUID bhea, UUID bres, byte[] resource) {
		if(storeid == null || size < 0 || name == null || sounds == null || sha1 == null || sha1.length != 20|| sha256 == null || sha256.length != 0x20) {
			AMusicLogger.warn("Pack save fail (invalid values)");
			return null;
		}
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			AMusicLogger.warn("Pack save fail (can not initialize SHA-1)");
			return null;
		}
		byte[] filesha1 = sha1hash.digest(resource);
		if(!Arrays.equals(filesha1, sha1)) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" save fail (invalid checksum)"));
			return null;
		}
		Path ampifile;
		try {
			ampifile = datadirectory.resolve(storeid.concat(FORMAT));
		} catch (InvalidPathException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" save fail (invalid path)"));
			return null;
		}
		int skip = 100;
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
			entryfilesize>>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			entryfilesize>>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			entryfilesize>>>=8;
			os.write((byte)entryfilesize); //FILESIZE
			os.write(sha1); //SHA1
			os.write(sha256); //SHA256
			byte[] buuids = new byte[0x20];
			{
				int k = buuids.length;
				long msb = bhea.getMostSignificantBits(), lsb = bhea.getLeastSignificantBits();
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				msb = bres.getMostSignificantBits();
				lsb = bres.getLeastSignificantBits();
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				msb>>>=8;
				buuids[--k] = (byte) msb;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
				lsb>>>=8;
				buuids[--k] = (byte) lsb;
			}
			os.write(buuids);
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
			soundcountb[1] = (byte) (soundcount>>>8);
			os.write(soundcountb);
			int lengthscount = soundcount<<1;
			skip += soundcount<<4; //SOUND HASH
			skip += soundcount<<2;
			byte[] soundhashs = new byte[soundcount<<4], namelengths = new byte[soundcount], splits = new byte[soundcount],lengths = new byte[lengthscount];
			int i = soundcount, j = soundcount<<1, k = soundcount<<4;
			int totalsoundnamelength = 0;
			byte[][] anames = new byte[soundcount][];
			while(--i > -1) {
				SoundInfo info = sounds[i];
				UUID hash = info.hash;
				long msb = hash.getMostSignificantBits(), lsb = hash.getLeastSignificantBits();
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				msb>>>=8;
				soundhashs[--k] = (byte) msb;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				lsb>>>=8;
				soundhashs[--k] = (byte) lsb;
				
				byte[] soundnamebytes = info.name.getBytes(StandardCharsets.UTF_8);
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
				splits[i] = info.split;
				short length = info.length;
				lengths[--j] = (byte) length;
				length >>>= 8;
				lengths[--j] = (byte) length;
			}
			skip += totalsoundnamelength;
			byte[] names = new byte[totalsoundnamelength];
			int namesi = 0;
			i = soundcount;
			while(--i > -1) {
				byte[] soundnamebytes = anames[i];
				int soundnamelength = soundnamebytes.length;
				System.arraycopy(soundnamebytes, 0, names, namesi, soundnamelength);
				namesi+=soundnamelength;
			}
			os.write(soundhashs); //SOUND HASHS ENTRY
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
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" save fail (IO exception)"));
			AMusicLogger.error(e1.getMessage());
			return null;
		}
		if(this.storeinram) {
			RamDataEntry dataentry = new RamDataEntry(storeid, size, name, sounds, sha1, sha256, bhea, bres, resource);
			return dataentry;
		}
		DefaultDataEntry entry = new DefaultDataEntry(skip, ampifile, storeid, size, name, sounds, sha1, sha256, bhea, bres);
		return entry;
	}
	
	public ResourcePacker createPacker(final String id) {
		if(this.lockwrite || id == null || this.soundsource == null || !this.soundsource.exists(id)) {
			return null;
		}
		final String filteredid = filterName(id);
		ResourcePacker packer = new ResourcePacker(this.soundsource, filteredid, this.packsource, false);
		return packer;
	}
	
	public boolean update(final String name, final ResourcePacker packer) {
		if(this.lockwrite || name == null) {
			return false;
		}
		if(packer == null) {
			final boolean deleted;
			DataEntry data = options.remove(name);
			if(data == null) {
				return false;
			}
			Path ampifile;
			try {
				ampifile = datadirectory.resolve(data.storeid.concat(FORMAT));
			} catch (InvalidPathException e) {
				AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" save fail (invalid path)"));
				return false;
			}
			try {
				deleted = fs.deleteIfExists(ampifile);
			} catch (IOException e) {
				AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" remove fail (IO exception)"));
				AMusicLogger.error(e.getMessage());
				return false;
			}
			if(deleted) {
				AMusicLogger.info("Pack \"".concat(data.storeid).concat("\" remove success"));
				this.printRamUsage();
			} else {
				AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" remove fail (not exsist)"));
			}
			return deleted;
		}
		packer.run();
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return false;
		}
		DataEntry oentry = options.remove(name);
		String storeid = oentry == null ? UUID.randomUUID().toString() : oentry.storeid;
		DataEntry entry = saveAmp(storeid, resourcepack.length, name, packer.sounds, packer.sha1, packer.sha256, packer.bhea, packer.bres, resourcepack);
		if(entry == null) {
			return false;
		}
		options.put(entry.name, entry);
		AMusicLogger.info("Pack \"".concat(storeid).concat("\" save success"));
		this.printRamUsage();
		return true;
	}
	
	private void printRamUsage() {
		if(storeinram) {
			long rambytesused = 0;
			for(DataEntry optionentry : options.values()) {
				rambytesused += optionentry.size;
			}
			String unit;
			if(rambytesused<0x800L) {
				unit = Long.toString(rambytesused).concat(" B");
			} else if(rambytesused<0x200000L) {
				unit = Long.toString((rambytesused>>>10)).concat(" KiB");
			} else if(rambytesused<0x80000000L) {
				unit = Long.toString((rambytesused>>>20)).concat(" MiB");
			} else if(rambytesused<0x20000000000L) {
				unit = Long.toString((rambytesused>>>30)).concat(" GiB");
			} else {
				unit = Long.toString((rambytesused>>>40)).concat(" TiB");
			}
			AMusicLogger.info("RAM used: ".concat(unit));
		}
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
