package me.bomb.amusic.resourcepack.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;

public final class LocalDataEntry extends DataEntry implements CustomDatastore {
	
	private static final HashSet<OpenOption> OPENOPTION_READ;
	
	static {
		HashSet<OpenOption> set = new HashSet<OpenOption>(1);
		set.add(StandardOpenOption.READ);
		OPENOPTION_READ = set;
	}
	
	private final FileSystemProvider fsp;
	protected final Path datapath;

	protected LocalDataEntry(String storeid, ResourcepackInfoImpl info, Path datapath) {
		super(storeid, info);
		this.fsp = datapath.getFileSystem().provider();
		this.datapath = datapath;
	}
	
	@Override
	public ByteBuffer getPack() {
		final MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		ResourcepackInfoImpl info = this.info;
		InputStream is = null;
		ByteBuffer buf = ByteBuffer.allocateDirect(info.packsize);
		
		try {
			is = this.fsp.newInputStream(this.datapath);
			is.skip(info.infosize);
			ReadableByteChannel channel = Channels.newChannel(is);
			ByteBuffer buffer = ByteBuffer.allocate(info.packsize);
			while (buffer.hasRemaining() && channel.read(buffer) != -1);
			buffer.flip();
		} catch (IOException e1) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
		}
		sha1hash.update(buf.duplicate());
		byte[] filesha1 = sha1hash.digest();
		if (!Arrays.equals(filesha1, info.sha1)) {
			return null;
		}
		return buf;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		try (FileChannel fileChannel = this.fsp.newFileChannel(this.datapath, LocalDataEntry.OPENOPTION_READ)) {
			long pos = info.infosize, rem = info.packsize, n;
			while (rem > 0L) {
				n = fileChannel.transferTo(pos, rem, channel);
				if (n < 1L) {
					if (!channel.isOpen()) {
						throw new IOException("Connection closed by remote peer during resourcepack transfer");
					}
					Thread.yield();
				}
				pos += n;
				rem -= n;
			}
		}
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
		InputStream is = null;
		byte[] resourcepack = null;
		boolean fail = false;
		try {
			is = this.fsp.newInputStream(this.datapath);
			is.skip(info.infosize);
			resourcepack = new byte[info.packsize];
		} catch (IOException e) {
			fail = true;
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		if(fail) {
			return false;
		}
		info.customdata = customdata;
		OutputStream os = null;
		try {
			os = this.fsp.newOutputStream(this.datapath);
			ResourcepackInfoImpl.serialize(os, info);
			os.write(resourcepack); //RESOURCEPACK ARCHIVE
		} catch (IOException e1) {
			fail = true;
		} finally {
			if(os != null) {
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
