package me.bomb.amusic.resourcepack;

import java.nio.file.Path;

import static me.bomb.amusic.util.NameFilter.filterName;

public final class PackMergeSourceLocal implements PackMergeSource {
	
	private final PackMergeEntry globalpack;
	private final Path mergepackdirectory;
	private final int maxresourcepacksize;
	
	public PackMergeSourceLocal(PackMergeEntry globalpack, Path mergepackdirectory, int maxresourcepacksize) {
		this.globalpack = globalpack;
		this.mergepackdirectory = mergepackdirectory;
		this.maxresourcepacksize = maxresourcepacksize;
	}

	@Override
	public PackMergeEntry get(String id) {
		if(id == null) {
			return this.globalpack;
		}
		final Path parentpack = this.mergepackdirectory.resolve(filterName(id).concat(".zip"));
		return parentpack.getFileSystem().provider().exists(parentpack) ? new PackMergeEntryFile(parentpack, this.maxresourcepacksize) : this.globalpack;
	}

}
