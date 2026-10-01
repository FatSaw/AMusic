package me.bomb.amusic.resourcepack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import me.bomb.amusic.resourcepack.LocalSoundSource.PackedResourcepack;

public class NoDataEntry extends DataEntry {

	private final LocalSoundSource lczs;
	private final PackMergeSource pms;
	
	protected NoDataEntry(String storeid, ResourcepackInfoImpl info, LocalSoundSource lczs, PackMergeSource pms) {
		super(storeid, info);
		this.lczs = lczs;
		this.pms = pms;
	}

	@Override
	public byte[] getPack() {
		final PackedResourcepack packedresourcepack = this.lczs.get(info.packname, this.pms.get(info.packname));
		if(packedresourcepack == null) {
			return null;
		}
		byte[] resourcepack = packedresourcepack.resourcepack;
		MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		sha1hash.update(resourcepack);
		byte[] sha1 = sha1hash.digest();
		return Arrays.equals(sha1, info.sha1) ? resourcepack : null;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		channel.write(ByteBuffer.wrap(this.getPack()));
	}
	
	@Override
	public int getLength() {
		return this.info.packsize;
	}

}
