package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import me.bomb.amusic.util.AMusicLogger;

public final class LocalDataEntry extends DataEntry implements CustomDatastore {
	
	private final FileSystemProvider fsp;
	protected final Path datapath;

	protected LocalDataEntry(String storeid, ResourcepackInfo info, Path datapath) {
		super(storeid, info);
		this.fsp = datapath.getFileSystem().provider();
		this.datapath = datapath;
	}
	
	@Override
	public byte[] getPack() {
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		ResourcepackInfo info = this.info;
		InputStream is = null;
		byte[] buf = new byte[info.packsize];
		try {
			is = this.fsp.newInputStream(this.datapath);
			is.skip(info.infosize);
			is.read(buf, 0, info.packsize);
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
			return null;
		}
		byte[] filesha1 = sha1hash.digest(buf);
		if (!Arrays.equals(filesha1, info.sha1)) {
			AMusicLogger.warn("Packed resourcepack \"".concat(storeid).concat("\" load fail (invalid checksum)"));
			return null;
		}
		return buf;
	}
	
	@Override
	public boolean updateCustomdata(byte[] customdata) {
		if(customdata == null || customdata.length > 0xFFFF) {
			AMusicLogger.warn("Pack update customdata fail (invalid values)");
			return false;
		}
		ResourcepackInfo info = this.info;
		String storeid = this.storeid;
		InputStream is = null;
		byte[] resourcepack;
		try {
			is = this.fsp.newInputStream(this.datapath);
			is.skip(info.infosize);
			resourcepack = new byte[info.packsize];
		} catch (IOException e) {
			AMusicLogger.warn("Pack \"".concat(storeid).concat("\" update customdata fail (invalid path)"));
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
		info.customdata = customdata;
		OutputStream os = null;
		try {
			os = this.fsp.newOutputStream(this.datapath);
			ResourcepackInfo.serialize(os, info);
			os.write(resourcepack); //RESOURCEPACK ARCHIVE
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
		return true;
	}

}
