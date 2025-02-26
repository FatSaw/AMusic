package me.bomb.amusic.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public final class LocalConvertedSource extends SoundSource {
	
	private final File musicdir;
	private final int maxsoundsize;
	
	public LocalConvertedSource(File musicdir, int maxsoundsize) {
		this.musicdir = musicdir;
		this.maxsoundsize = maxsoundsize;
	}

	@Override
	public SourceEntry get(String entrykey) {
		File musicdir = new File(this.musicdir, entrykey);
		if(musicdir == null || !musicdir.exists()) return null;
		ArrayList<File> musicfiles = new ArrayList<>();
		ArrayList<String> soundnames = new ArrayList<>();
		for (File musicfile : musicdir.listFiles()) {
			if (!musicfile.getName().endsWith(".ogg") || musicfile.length()  > maxsoundsize) continue;
			musicfiles.add(musicfile);
			String songname = musicfile.getName();
			int i = songname.lastIndexOf(".");
			if (i != -1) {
				songname = songname.substring(0, i);
			}
			soundnames.add(songname);
		}
		int i = musicfiles.size();
		String[] names = soundnames.toArray(new String[i]);
		short[] lengths = new short[i];
		byte[][] data = new byte[i][];
		boolean[] success = new boolean[i];
		SourceEntry source = new SourceEntry(names, lengths, data, null, success);
		while(--i > -1) {
			File infile = musicfiles.get(i);
			try {
				long filesize = infile.length();
				byte[] resource = new byte[(int) filesize];
				FileInputStream in = new FileInputStream(infile);
				int size = in.read(resource);
				if(size < filesize) {
					resource = Arrays.copyOf(resource, size);
				}
				in.close();
				lengths[i] = calculateDuration(resource);
				data[i] = resource;
				success[i] = true;
			} catch (IOException e) {
			}
		}
		return source;
	}

	@Override
	public boolean exists(String entrykey) {
		File musicdir = new File(this.musicdir, entrykey);
		if(musicdir == null || !musicdir.exists()) return false;
		for (File musicfile : musicdir.listFiles()) {
			if (!musicfile.getName().endsWith(".ogg") || musicfile.length() > maxsoundsize) continue;
			return true;
		}
		return false;
	}

	@Override
	public Path getSource() {
		return this.musicdir.toPath();
	}

}
