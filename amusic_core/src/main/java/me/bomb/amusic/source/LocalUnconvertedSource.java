package me.bomb.amusic.source;

import static me.bomb.amusic.util.NameFilter.filterName;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalUnconvertedSource extends SoundSource {
	
	private static final DirectoryStream.Filter<Path> filefilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path path) throws IOException {
			return !path.getFileSystem().provider().readAttributes(path, BasicFileAttributes.class).isDirectory();
		}
    }, dirfilter = new DirectoryStream.Filter<Path>() {
    	@Override
		public boolean accept(Path path) throws IOException {
			return path.getFileSystem().provider().readAttributes(path, BasicFileAttributes.class).isDirectory();
		}
    };

	private final FileSystemProvider fs;
	private final Runtime runtime;
	private final Path musicdir, fmpegbinary;
	private final int maxsoundsize, bitrate, samplingrate;
	private final byte channels;
	private final float threadcoefficient;
	private final short threadcountlimit;
	
	public LocalUnconvertedSource(Runtime runtime, Path musicdir, int maxsoundsize, Path fmpegbinary, int bitrate, byte channels, int samplingrate, float threadcoefficient, short threadcountlimit) {
		this.fs = musicdir.getFileSystem().provider();
		this.runtime = runtime;
		this.musicdir = musicdir;
		this.maxsoundsize = maxsoundsize;
		this.fmpegbinary = fmpegbinary;
		this.bitrate = bitrate;
		this.samplingrate = samplingrate;
		this.channels = channels;
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
		String[] args = new String[] {fmpegbinary.toAbsolutePath().toString(), "-i", null, "-strict", "-2", "-acodec", "vorbis", "-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate), "-f", "ogg", "-vn", "-y", "pipe:1"};
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
		final SourceEntry source;
		try {
			HashSet<Path> filesm = new HashSet<>();
			ds = fs.newDirectoryStream(musicdir, filefilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				filesm.add(it.next());
			}
			ds.close();
			i = filesm.size();
			files = new Path[i];
			names = new String[i];
			lengths = new short[i];
			data = new byte[i][];
			finished = usemt ? new AtomicBoolean[i] : null;
			success = new boolean[i];
			source = new SourceEntry(names, lengths, data, finished, success);
			Iterator<Path> fiterator = filesm.iterator();
			while(--i > -1) {
				final Path file = fiterator.next();
				files[i] = file;
				String songname = file.getFileName().toString();
				final int j = songname.lastIndexOf(".");
				if (j != -1) {
					songname = songname.substring(0, j);
				}
				names[i] = songname;
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
			if(threadcount == 0) {
				++threadcount;
			}
			if(threadcount > threadcountlimit) {
				threadcount = threadcountlimit;
			}
			int savecount = 0;
			try {
				savecount = filecount/threadcount;
			} catch(ArithmeticException e) {
				return null;
			}
			int remain = filecount - savecount * threadcount;
			int nums = 0;
			while(--threadcount > -1) {
				final int pnums = nums;
				if(--remain > -1) {
					++nums;
				}
				new Thread(new ConvertSound(runtime, args, maxsoundsize, null, files, data, lengths, finished, success, pnums, filecount > (nums+=savecount) ? nums : filecount)).start();
			}
		} else {
			new ConvertSound(runtime, args, maxsoundsize, null, files, data, lengths, finished, success, 0, files.length).run();
		}
		return source;
	}

	@Override
	public boolean exists(String entrykey) {
		Path musicdir = this.musicdir.resolve(entrykey);
		if(musicdir == null) {
			return false;
		}
		DirectoryStream<Path> ds = null;
		try {
			ds = fs.newDirectoryStream(musicdir, filefilter);
			final boolean exists = ds.iterator().hasNext();
			ds.close();
			return exists;
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		return false;
	}

	@Override
	public String[] getPlaylists() {
		DirectoryStream<Path> ds = null;
		ArrayList<String> playlists = new ArrayList<String>();
		try {
			ds = fs.newDirectoryStream(musicdir, dirfilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path playlistdir = it.next().getFileName();
				playlists.add(playlistdir.toString());
			}
			ds.close();
			return playlists.toArray(new String[playlists.size()]);
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		return null;
	}

	@Override
	public String[] getSounds(String playlistname) {
		playlistname = filterName(playlistname);
		
		Path musicdir = this.musicdir.resolve(playlistname);
		if(musicdir == null) {
			return null;
		}
		DirectoryStream<Path> ds = null;
		ArrayList<String> tracks = new ArrayList<String>();
		try {
			ds = fs.newDirectoryStream(musicdir, filefilter);
			final Iterator<Path> it = ds.iterator();
			while(it.hasNext()) {
				final Path oggfile = it.next();
				try {
					BasicFileAttributes attributes = fs.readAttributes(oggfile, BasicFileAttributes.class);
					final long size = attributes.size();
					if(attributes.isDirectory() || size > maxsoundsize) {
						continue;
					}
					String trackname = musicdir.getFileName().toString();
					int index = trackname.lastIndexOf('.');
					if(index != -1) {
						trackname = trackname.substring(0, index);
					}
					tracks.add(trackname);
				} catch (IOException e) {
					continue;
				}
			}
			ds.close();
		} catch(IOException e1) {
			if(ds != null) {
				try {
					ds.close();
				} catch(IOException e2) {
				}
			}
		}
		return tracks.size() == 0 ? null : tracks.toArray(new String[tracks.size()]);
	}

}
