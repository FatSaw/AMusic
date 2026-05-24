package me.bomb.amusic.packedinfo;

import static me.bomb.amusic.util.NameFilter.filterName;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.util.AMusicLogger;
import me.bomb.amusic.util.HexUtils;

public class NoStorage extends me.bomb.amusic.packedinfo.Data {
	
	

	protected NoStorage(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram) {
		super(soundsource, packsource, lockwrite, storeinram);
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
		String[] playlists = this.soundsource.getPlaylists();
		int i = playlists.length;
		while(--i > -1) {
			String playlist = playlists[i];
			if(playlist == null) {
				continue;
			}
			final String filteredid = filterName(playlist);
			ResourcePacker packer = new ResourcePacker(this.soundsource, filteredid, this.packsource, true);
			packer.run();
			final byte[] resourcepack;
			if((resourcepack = packer.resourcepack) == null) {
				continue;
			}
			options.put(playlist, new RamDataEntry(null, resourcepack.length, playlist, packer.sounds, packer.sha1, resourcepack));
			AMusicLogger.info("Packed resourcepack, hash: ".concat(HexUtils.fromBytesToHex(packer.sha1)));
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
	
	/**
	 * Ignored.
	 */
	@Override
	public void end() {
	}

	@Override
	public ResourcePacker createPacker(String id) {
		if(this.lockwrite || id == null || this.soundsource == null || !this.soundsource.exists(id)) {
			return null;
		}
		final String filteredid = filterName(id);
		ResourcePacker packer = new ResourcePacker(this.soundsource, filteredid, this.packsource, true);
		return packer;
	}

	@Override
	public boolean update(String name, ResourcePacker packer) {
		if(this.lockwrite || name == null) {
			return false;
		}
		if(packer == null) {
			DataEntry data = options.remove(name);
			if(data == null) {
				return false;
			}
			this.printRamUsage();
			return true;
		}
		packer.run();
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return false;
		}
		options.put(name, new RamDataEntry(null, resourcepack.length, name, packer.sounds, packer.sha1, resourcepack));
		this.printRamUsage();
		return true;
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
