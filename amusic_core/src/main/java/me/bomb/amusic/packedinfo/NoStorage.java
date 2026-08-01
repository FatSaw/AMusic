package me.bomb.amusic.packedinfo;

import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource.PackedResourcepack;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.HexUtils;

public class NoStorage extends me.bomb.amusic.packedinfo.Data {
	
	private final LocalConvertedZerocopySource lczs;
	
	
	protected NoStorage(boolean lockwrite, LocalConvertedZerocopySource lczs) {
		super(lockwrite);
		this.lczs = lczs;
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
		String[] resourcepacks = this.lczs.listResourcepacks();
		int i = resourcepacks.length;
		while(--i > -1) {
			String resourcepack = resourcepacks[i];
			if(resourcepack == null) {
				continue;
			}
			final PackedResourcepack packedresourcepack = this.lczs.get(resourcepack);
			
			if(packedresourcepack == null) {
				continue;
			}
			ResourcepackInfo info = packedresourcepack.info;
			options.put(resourcepack, new NoDataEntry(null, info, this.lczs));
			AMusicLogger.info("Packed resourcepack, hash: ".concat(HexUtils.fromBytesToHex(info.sha1)));
		}
		AMusicLogger.info("Packed ".concat(Integer.toString(options.size())).concat(" resourcepacks"));
		this.printRamUsageInfo();
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
			this.printRamUsageInfo();
			return UpdateResult.DELETED_SUCCESS;
		}
		if(packer.resourcepack == null) {
			return UpdateResult.PACKED_FAILED;
		}
		options.put(id, new NoDataEntry(null, packer.info, this.lczs));
		this.printRamUsageInfo();
		return UpdateResult.PACKED_SUCCESS;
	}

}
