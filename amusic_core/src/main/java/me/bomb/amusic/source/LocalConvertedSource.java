package me.bomb.amusic.source;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalConvertedSource extends SoundSource {
	
	private static final DirectoryStream.Filter<Path> oggfilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			final String name = path.getFileName().toString();
			return name.startsWith(".ogg", name.length() - 4);
		}
    };
	
	private final FileSystemProvider fs;
	private final Path musicdir;
	private final int maxsoundsize;
	private final float threadcoefficient;
	private final short threadcountlimit;
	
	public LocalConvertedSource(Path musicdir, int maxsoundsize, float threadcoefficient, short threadcountlimit) {
		this.fs = musicdir.getFileSystem().provider();
		this.musicdir = musicdir;
		this.maxsoundsize = maxsoundsize;
		if(threadcountlimit < 0) {
			threadcountlimit = 0;
		}
		if(threadcoefficient != Float.NaN) {
			if(threadcoefficient < 0.0f) {
				threadcoefficient = 0.0f;
			}
			if(threadcoefficient > 1.0f) {
				threadcoefficient = 1.0f;
			}
		}
		this.threadcoefficient = threadcoefficient;
		this.threadcountlimit = threadcountlimit;
	}

	@Override
	public SourceEntry get(String entrykey) {
		Path musicdir = this.musicdir.resolve(entrykey);
		if(musicdir == null) return null;
		DirectoryStream<Path> ds = null;
		final boolean usemt = threadcountlimit > 0 && threadcoefficient != Float.NaN;
		int i;
		final Path[] files;
		final String[] names;
		final short[] lengths;
		final byte[][] data;
		final AtomicBoolean[] finished;
		final boolean[] success;
		final int[] sizes;
		final SourceEntry source;
		try {
			HashMap<Path, Integer> filesm = new HashMap<>();
			ds = fs.newDirectoryStream(musicdir, oggfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fs.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(attributes.isDirectory() || size > maxsoundsize) {
						continue;
					}
					filesm.put(oggfile, (int)size);
				} catch (IOException e) {
					continue;
				}
			}
			ds.close();
			i = filesm.size();
			files = new Path[i];
			names = new String[i];
			lengths = new short[i];
			data = new byte[i][];
			finished = usemt ? new AtomicBoolean[i] : null;
			success = new boolean[i];
			sizes = new int[i];
			source = new SourceEntry(names, lengths, data, finished, success);
			Iterator<Entry<Path, Integer>> fiterator = filesm.entrySet().iterator();
			while(--i > -1) {
				final Entry<Path, Integer> filee = fiterator.next();
				final Path file = filee.getKey();
				files[i] = file;
				String songname = file.getFileName().toString();
				final int j = songname.lastIndexOf(".");
				if (j != -1) {
					songname = songname.substring(0, j);
				}
				names[i] = songname;
				sizes[i] = filee.getValue();
				if(!usemt) {
					continue;
				}
				finished[i] = new AtomicBoolean();
			}
		} catch (IOException e) {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e1) {
				}
			}
			return null;
		}
		if(usemt) {
			int filecount = files.length;
			int threadcount = (int) (threadcoefficient * filecount);
			++threadcount;
			if(threadcount > threadcountlimit) {
				threadcount = threadcountlimit;
			}
			int savecount = 0;
			try {
				savecount = filecount/threadcount;
			} catch(ArithmeticException e) {
				return null;
			}
			++savecount;
			int nums = 0;
			while(--threadcount > -1) {
				final int pnums = nums;
				new Thread(new ReadSound(maxsoundsize, sizes, files, data, lengths, finished, success, pnums, filecount > (nums+=savecount) ? filecount : nums)).start();
			}
		} else {
			final int start = 0, end = files.length;
			ReadSound sr = new ReadSound(maxsoundsize, sizes, files, data, lengths, finished, success, start, end);
			sr.run();
		}
		return source;
	}

	@Override
	public boolean exists(String entrykey) {
		Path musicdir = this.musicdir.resolve(entrykey);
		DirectoryStream<Path> ds = null;
		try {
			ds = fs.newDirectoryStream(musicdir, oggfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fs.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(attributes.isDirectory() || size > maxsoundsize) {
						continue;
					}
					ds.close();
					return true;
				} catch (IOException e) {
					continue;
				}
			}
		} catch(IOException e) {
		} finally {
			try {
				ds.close();
			} catch(IOException e) {
			}
		}
		return false;
	}

	@Override
	public Path getSource() {
		return this.musicdir;
	}

}
