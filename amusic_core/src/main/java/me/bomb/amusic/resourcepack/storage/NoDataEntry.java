package me.bomb.amusic.resourcepack.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import me.bomb.amusic.resourcepack.packer.PackMergeSource;
import me.bomb.amusic.resourcepack.packer.PackedResourcepack;
import me.bomb.amusic.resourcepack.packer.ResourcepackCreator;

public class NoDataEntry extends DataEntry {

	private final ResourcepackCreator lczs;
	private final PackMergeSource pms;
	
	protected NoDataEntry(String storeid, ResourcepackInfoImpl info, ResourcepackCreator lczs, PackMergeSource pms) {
		super(storeid, info);
		this.lczs = lczs;
		this.pms = pms;
	}

	@Override
	public ByteBuffer getPack() {
		final PackedResourcepack packedresourcepack = this.lczs.get(info.packname, this.pms.get(info.packname));
		if(packedresourcepack == null) {
			return null;
		}
		ByteBuffer resourcepack = packedresourcepack.resourcepack;
		MessageDigest sha1hash;
		try {
			sha1hash = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		sha1hash.update(resourcepack.duplicate());
		byte[] sha1 = sha1hash.digest();
		return Arrays.equals(sha1, info.sha1) ? resourcepack.duplicate() : null;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		channel.write(this.getPack());
	}
	
	@Override
	public int getLength() {
		return this.info.packsize;
	}

}
