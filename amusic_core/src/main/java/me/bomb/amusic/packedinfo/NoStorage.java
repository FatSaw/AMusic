package me.bomb.amusic.packedinfo;

import static me.bomb.amusic.util.NameFilter.filterName;

import java.util.UUID;

import me.bomb.amusic.resource.ResourcePacker;
import me.bomb.amusic.source.PackSource;
import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.util.AMusicLogger;

public class NoStorage extends me.bomb.amusic.packedinfo.Data {
	
	private final SoundSource soundsource;
	private final PackSource packsource;

	protected NoStorage(SoundSource soundsource, PackSource packsource, boolean lockwrite, boolean storeinram) {
		super(lockwrite, storeinram);
		this.soundsource = soundsource;
		this.packsource = packsource;
	}
	
	/**
	 * Ignored.
	 */
	@Override
	protected void save() {
	}
	
	/**
	 * Ignored.
	 */
	@Override
	public void load() {
		String[] playlists = this.soundsource.getPlaylists();
		int i = playlists.length;
		while(--i > -1) {
			String playlist = playlists[i];
			ResourcePacker packer = this.createPacker(playlist, this.soundsource, this.packsource);
			if(packer == null) {
				continue;
			}
			if(!this.update(playlist, packer)) {
				continue;
			}
		}
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

	/**
	 * SoundSource ignored
	 * PackSource ignored
	 */
	@Override
	public ResourcePacker createPacker(String id, SoundSource soundsource, PackSource packsource) {
		if(id == null || this.soundsource == null || !this.soundsource.exists(id)) {
			return null;
		}
		final String filteredid = filterName(id);
		ResourcePacker packer = new ResourcePacker(this.soundsource, filteredid, this.packsource);
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
			AMusicLogger.info("Pack \"".concat(data.storeid).concat("\" temp remove success"));
			this.printRamUsage();
			return true;
		}
		packer.run();
		final byte[] resourcepack;
		if((resourcepack = packer.resourcepack) == null) {
			return false;
		}
		DataEntry oentry = options.remove(name);
		String storeid = oentry == null ? UUID.randomUUID().toString() : oentry.storeid;
		DataEntry entry = new RamDataEntry(storeid, resourcepack.length, name, packer.sounds, packer.sha1, resourcepack);
		options.put(entry.name, entry);
		AMusicLogger.info("Pack \"".concat(storeid).concat("\" temp save success"));
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
