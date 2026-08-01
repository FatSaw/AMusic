package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource.PackedResourcepack;
import me.bomb.amusic.util.AMusicLogger;

public class LocalCachedStorage extends me.bomb.amusic.packedinfo.Data {
	
	private static final String FORMAT = ".ampi";
	private static final byte FORMATSIZE = 5;
	private static final DirectoryStream.Filter<Path> ampifilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			final String name = path.getFileName().toString();
			return name.startsWith(FORMAT, name.length() - FORMATSIZE);
		}
    };

	private final LocalConvertedZerocopySource lczs;
	private final FileSystemProvider fsp;
	private final Path packeddirectory;
	
	protected LocalCachedStorage(boolean lockwrite, LocalConvertedZerocopySource lczs, Path packeddirectory) {
		super(lockwrite);
		this.lczs = lczs;
		this.fsp = packeddirectory.getFileSystem().provider();
		this.packeddirectory = packeddirectory;
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
			ds = this.fsp.newDirectoryStream(this.packeddirectory, ampifilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path ampifile = it.next();
				String storeid = ampifile.getFileName().toString();
				storeid = storeid.substring(0, storeid.length() - FORMATSIZE);
				LocalCachedDataEntry entry = loadAmp(storeid);
				if(entry == null) {
					continue;
				}
				options.put(entry.info.packname, entry);
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
	
	private LocalCachedDataEntry loadAmp(String storeid) {
		if(storeid == null) {
			AMusicLogger.warn("Pack load fail (invalid values)");
			return null;
		}
		Path ampifile;
		try {
			ampifile = packeddirectory.resolve(storeid.concat(FORMAT));
		} catch (InvalidPathException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid path)"));
			throw new IllegalStateException(e);
		}
		InputStream is = null;
		ResourcepackInfo info;
		byte[] resourcepack = null;
		try {
			is = this.fsp.newInputStream(ampifile);
			info = ResourcepackInfo.deserialize(is);
			resourcepack = new byte[info.packsize];
			is.read(resourcepack, 0, resourcepack.length);
		} catch (IOException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" load fail (invalid path)"));
			throw new IllegalStateException(e);
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return new LocalCachedDataEntry(storeid, info, resourcepack, ampifile);
	}
	
	private DataEntry saveAmp(String storeid, ResourcepackInfo info, byte[] resource) {
		if(storeid == null || info.packsize < 0 || info.packname == null || info.sounds == null || info.sha1 == null || info.sha1.length != 20|| info.sha256 == null || info.sha256.length != 0x20) {
			AMusicLogger.warn("Pack save fail (invalid values)");
			return null;
		}
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			AMusicLogger.warn("Pack save fail (can not initialize SHA-1)");
			throw new IllegalStateException(e);
		}
		byte[] filesha1 = sha1hash.digest(resource);
		if(!Arrays.equals(filesha1, info.sha1)) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" save fail (invalid checksum)"));
			return null;
		}
		Path ampifile;
		try {
			ampifile = packeddirectory.resolve(storeid.concat(FORMAT));
		} catch (InvalidPathException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" save fail (invalid path)"));
			throw new IllegalStateException(e);
		}
		OutputStream os = null;
		try {
			os = this.fsp.newOutputStream(ampifile);
			ResourcepackInfo.serialize(os, info);
			os.write(resource); //RESOURCEPACK ARCHIVE
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		} finally {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return new LocalCachedDataEntry(storeid, info, resource, ampifile);
	}

	/**
	 * Ignored.
	 */
	@Override
	public void start() {
		
	}

	@Override
	public void end() {
		options.clear();
	}

	@Override
	public UpdateResult update(String id) {
		if(this.lockwrite || id == null) {
			return UpdateResult.UNAVILABLE;
		}
		PackedResourcepack packer = this.lczs.get(id);
		if(packer == null) {
			DataEntry data = options.remove(id);
			if(data == null) {
				return UpdateResult.DELETED_FAILED;
			}
			this.printRamUsage();
			Path ampifile;
			try {
				ampifile = packeddirectory.resolve(data.storeid.concat(FORMAT));
			} catch (InvalidPathException e) {
				AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" save fail (invalid path)"));
				return UpdateResult.DELETED_FAILED;
			}
			try {
				if(this.fsp.deleteIfExists(ampifile)) {
					AMusicLogger.info("Pack \"".concat(data.storeid).concat("\" remove success"));
					return UpdateResult.DELETED_SUCCESS;
				} else {
					AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" remove fail (not exsist)"));
					return UpdateResult.DELETED_FAILED;
				}
			} catch (IOException e) {
				AMusicLogger.warn("Pack \"".concat(data.storeid).concat("\" remove fail (IO exception)"));
				AMusicLogger.error(e.getMessage());
				return UpdateResult.DELETED_FAILED;
			}
		}
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return UpdateResult.PACKED_FAILED;
		}
		DataEntry oentry = options.remove(id);
		String storeid = oentry == null ? UUID.randomUUID().toString() : oentry.storeid;
		ResourcepackInfo info = packer.info;
		DataEntry entry = saveAmp(storeid, info, resourcepack);
		if(entry == null) {
			return UpdateResult.PACKED_FAILED;
		}
		options.put(id, entry);
		this.printRamUsage();
		return UpdateResult.PACKED_SUCCESS;
	}

}
