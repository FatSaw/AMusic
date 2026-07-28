package me.bomb.amusic.packedinfo;

import me.bomb.amusic.packedinfo.LocalConvertedZerocopySource.PackedResourcepack;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.HexUtils;

public class RamStorage extends me.bomb.amusic.packedinfo.Data {
	
	private final LocalConvertedZerocopySource lczs;
	
	
	protected RamStorage(boolean lockwrite, LocalConvertedZerocopySource lczs) {
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
		String[] playlists = this.lczs.getPlaylists();
		int i = playlists.length;
		while(--i > -1) {
			String playlist = playlists[i];
			if(playlist == null) {
				continue;
			}
			final PackedResourcepack packedresourcepack = this.lczs.get(playlist);
			
			if(packedresourcepack == null) {
				continue;
			}
			SoundInfo[] soundinfos = new SoundInfo[packedresourcepack.names.length];
			int j = soundinfos.length;
			while(--j > -1) {
				soundinfos[j] = new SoundInfo(packedresourcepack.names[j], packedresourcepack.soundhashs[j], packedresourcepack.lengths[j], packedresourcepack.splits[j]);
			}
			options.put(playlist, new RamDataEntry(null, packedresourcepack.resourcepack.length, playlist, soundinfos, packedresourcepack.sha1, packedresourcepack.sha256, packedresourcepack.bhea, packedresourcepack.bres, packedresourcepack.resourcepack));
			AMusicLogger.info("Packed resourcepack, hash: ".concat(HexUtils.fromBytesToHex(packedresourcepack.sha1)));
		}
		AMusicLogger.info("Packed ".concat(Integer.toString(options.size())).concat(" resourcepacks"));
		this.printRamUsage();
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
		PackedResourcepack packer = lczs.get(id);
		if(packer == null) {
			DataEntry data = options.remove(id);
			if(data == null) {
				return UpdateResult.DELETED_FAILED;
			}
			this.printRamUsage();
			return UpdateResult.DELETED_SUCCESS;
		}
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return UpdateResult.PACKED_FAILED;
		}
		
		SoundInfo[] soundinfos = new SoundInfo[packer.names.length];
		int j = soundinfos.length;
		while(--j > -1) {
			soundinfos[j] = new SoundInfo(packer.names[j], packer.soundhashs[j], packer.lengths[j], packer.splits[j]);
		}
		
		options.put(id, new RamDataEntry(null, resourcepack.length, id, soundinfos, packer.sha1, packer.sha256, packer.bhea, packer.bres, resourcepack));
		this.printRamUsage();
		return UpdateResult.PACKED_SUCCESS;
	}
	
	private void printRamUsage() {
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
