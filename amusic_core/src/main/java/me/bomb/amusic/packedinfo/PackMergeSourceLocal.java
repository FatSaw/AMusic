package me.bomb.amusic.packedinfo;

import java.nio.file.Path;

import static me.bomb.amusic.util.NameFilter.filterName;

public final class PackMergeSourceLocal implements PackMergeSource {
	
	//private final FileSystemProvider fsp;
	//private final RegularFileFilter regularfilefilter;
	private final PackMergeEntry globalpack;
	private final Path mergepackdirectory;
	private final int maxresourcepacksize;
	
	public PackMergeSourceLocal(PackMergeEntry globalpack, Path mergepackdirectory, int maxresourcepacksize) {
		//this.fsp = mergepackdirectory.getFileSystem().provider();
		//this.regularfilefilter = new RegularFileFilter(this.fsp);
		this.globalpack = globalpack;
		this.mergepackdirectory = mergepackdirectory;
		this.maxresourcepacksize = maxresourcepacksize;
	}

	@Override
	public PackMergeEntry get(String id) {
		if(id == null) {
			return this.globalpack;
		}
		/*id = id.concat(".zip");
		DirectoryStream<Path> ds = null;
		Path parentpack = null;
		try {
			ds = fsp.newDirectoryStream(this.mergepackdirectory, this.regularfilefilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path dir = it.next();
				if(dir.getFileName().toString().equals(id)) {
					parentpack = dir;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return this.globalpack;
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
		}
		ds = null;*/
		
		final Path parentpack = this.mergepackdirectory.resolve(filterName(id).concat(".zip"));
		return new PackMergeEntryFile(parentpack, this.maxresourcepacksize);
	}
	
	/*private static final class RegularFileFilter implements DirectoryStream.Filter<Path> {
		private final FileSystemProvider fsp;
		private RegularFileFilter(FileSystemProvider fsp) {
			this.fsp = fsp;
		}
		@Override
		public boolean accept(Path path) throws IOException {
			return this.fsp.readAttributes(path, BasicFileAttributes.class).isRegularFile();
		}
	}*/

}
