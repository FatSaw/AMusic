package me.bomb.amusic.converter;

import java.io.File;

public final class ConvertationFiles {
	public final File input, output;
	protected boolean started = false, finished = false;
	public ConvertationFiles(File input, File output) {
		this.input = input;
		this.output = output;
	}
	public boolean finished() {
		return finished;
	}
}
