package me.bomb.amusic.packedinfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource.PackedResourcepack;

public class NoDataEntry extends DataEntry {

	private final LocalConvertedZerocopySource lczs;
	
	protected NoDataEntry(String storeid, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] sha256, UUID bhea, UUID bres, LocalConvertedZerocopySource lczs) {
		super(storeid, size, name, sounds, sha1, sha256, bhea, bres);
		this.lczs = lczs;
	}

	@Override
	public byte[] getPack() {
		final PackedResourcepack packedresourcepack = this.lczs.get(this.name);
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
		return Arrays.equals(sha1, this.sha1) ? resourcepack : null;
	}

}
