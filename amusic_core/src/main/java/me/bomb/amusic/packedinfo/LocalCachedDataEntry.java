package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import me.bomb.amusic.util.AMusicLogger;

public final class LocalCachedDataEntry extends DataEntry implements CustomDatastore {
	
	private final FileSystemProvider fsp;
	private byte[] pack;
	protected final Path datapath;

	protected LocalCachedDataEntry(String storeid, ResourcepackInfo info, byte[] pack, Path datapath) {
		super(storeid, info);
		this.fsp = datapath.getFileSystem().provider();
		this.pack = pack;
		this.datapath = datapath;
	}
	
	@Override
	public byte[] getPack() {
		return this.pack;
	}
	
	@Override
	public boolean updateCustomdata(byte[] customdata) {
		if(customdata == null || customdata.length > 0xFFFF) {
			AMusicLogger.warn("Pack update customdata fail (invalid values)");
			return false;
		}
		ResourcepackInfo info = this.info;
		info.customdata = customdata;
		OutputStream os = null;
		try {
			os = this.fsp.newOutputStream(this.datapath);
			ResourcepackInfo.serialize(os, info);
			os.write(this.pack); //RESOURCEPACK ARCHIVE
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
