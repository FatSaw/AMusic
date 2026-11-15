package me.bomb.amusic.resourcepack.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

public final class LocalCachedDataEntry extends DataEntry implements CustomDatastore {
	
	private final FileSystemProvider fsp;
	private ByteBuffer pack;
	protected final Path datapath;

	protected LocalCachedDataEntry(String storeid, ResourcepackInfoImpl info, ByteBuffer pack, Path datapath) {
		super(storeid, info);
		this.fsp = datapath.getFileSystem().provider();
		this.pack = pack;
		this.datapath = datapath;
	}
	
	@Override
	public ByteBuffer getPack() {
		return this.pack;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		channel.write(this.pack);
	}
	
	@Override
	public int getLength() {
		return this.info.packsize;
	}
	
	@Override
	public boolean updateCustomdata(byte[] customdata) {
		if(customdata == null || customdata.length > 0xFFFF) {
			return false;
		}
		ResourcepackInfoImpl info = this.info;
		info.customdata = customdata;
		OutputStream os = null;
		boolean fail = false;
		try {
			os = this.fsp.newOutputStream(this.datapath);
			ResourcepackInfoImpl.serialize(os, info);
			WritableByteChannel channel = Channels.newChannel(os);
			ByteBuffer buffer = this.pack;
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
		} catch (IOException e1) {
			fail = true;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return !fail;
	}

}
