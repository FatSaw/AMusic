package me.bomb.amusic.packedinfo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.bomb.amusic.util.AMusicLogger;

public abstract class Data {
	
	protected final Map<String,DataEntry> options = new HashMap<>();
	public final boolean lockwrite;
	
	
	protected Data(boolean lockwrite) {
		this.lockwrite = lockwrite;
	}
	
	/**
	 * Get no storage
	 * Generate resourcepack for each request
	 * @return no storage
	 */
	public static NoStorage getNoStorage(boolean lockwrite, LocalConvertedZerocopySource soundsource, PackMergeSource packmergesource) {
		return new NoStorage(lockwrite, soundsource, packmergesource);
	}
	
	/**
	 * Get ram storage
	 * Store resourcepack on ram
	 * @return ram storage
	 */
	public static RamStorage getRamStorage(boolean lockwrite, LocalConvertedZerocopySource soundsource, PackMergeSource packmergesource) {
		return new RamStorage(lockwrite, soundsource, packmergesource);
	}
	
	/**
	 * Get local storage
	 * Store resourcepack on disk
	 * @return local storage
	 */
	public static LocalStorage getLocalStorage(boolean lockwrite, LocalConvertedZerocopySource soundsource, PackMergeSource packmergesource, Path packeddirectory) {
		return new LocalStorage(lockwrite, soundsource, packmergesource, packeddirectory);
	}
	
	/**
	 * Get local cached storage
	 * Store resourcepack on disk and on ram
	 * @return local cached storage
	 */
	public static LocalCachedStorage getLocalCachedStorage(boolean lockwrite, LocalConvertedZerocopySource soundsource, PackMergeSource packmergesource, Path packeddirectory) {
		return new LocalCachedStorage(lockwrite, soundsource, packmergesource, packeddirectory);
	}
	
	/**
	 * Save {@link Data#options} to storage.
	 */
	protected abstract void save();

	/**
	 * Load {@link Data#options} from storage.
	 */
	public abstract void load();
	
	public abstract void start();
	
	public abstract void end();
	
	/**
	 * Update packed info
	 * @param id
	 * @return not null if write allowed
	 */
	public abstract UpdateResult update(final String id);
	

	public final DataEntry getResourcepack(String playlistname) {
		return options.get(playlistname);
	}

	public final boolean containsResourcepack(String playlistname) {
		return options.containsKey(playlistname);
	}

	public final String[] listResourcepacks() {
		int i = options.size();
		String[] playlists = new String[i];
		Iterator<String> iterator = options.keySet().iterator();
		while(iterator.hasNext() && --i > -1) {
			playlists[i] = iterator.next();
		}
		return playlists;
	}
	
	protected void printRamUsageInfo() {
		long rambytesused = 0;
		for(DataEntry optionentry : options.values()) {
			rambytesused += optionentry.info.infosize;
		}
		StringBuilder sb = new StringBuilder("RAM used(info): ");
		if(rambytesused<0x800L) {
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes");
		} else if(rambytesused<0x200000L) {
			sb.append(Long.toString((rambytesused>>>10)));
			sb.append(" KiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else if(rambytesused<0x80000000L) {
			sb.append(Long.toString((rambytesused>>>20)));
			sb.append(" MiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else if(rambytesused<0x20000000000L) {
			sb.append(Long.toString((rambytesused>>>30)));
			sb.append(" GiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else {
			sb.append(Long.toString((rambytesused>>>40)));
			sb.append(" TiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		}
		AMusicLogger.info(sb.toString());
	}
	
	protected void printRamUsage() {
		long rambytesused = 0;
		for(DataEntry optionentry : options.values()) {
			rambytesused += optionentry.info.infosize;
			rambytesused += optionentry.info.packsize;
		}
		StringBuilder sb = new StringBuilder("RAM used(info+data): ");
		if(rambytesused<0x800L) {
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes");
		} else if(rambytesused<0x200000L) {
			sb.append(Long.toString((rambytesused>>>10)));
			sb.append(" KiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else if(rambytesused<0x80000000L) {
			sb.append(Long.toString((rambytesused>>>20)));
			sb.append(" MiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else if(rambytesused<0x20000000000L) {
			sb.append(Long.toString((rambytesused>>>30)));
			sb.append(" GiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		} else {
			sb.append(Long.toString((rambytesused>>>40)));
			sb.append(" TiB");
			sb.append(" (");
			sb.append(Long.toString(rambytesused));
			sb.append(" bytes)");
		}
		AMusicLogger.info(sb.toString());
	}
	
}
