package me.bomb.amusic;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;

final class Converter extends Encoder implements Runnable {
	private static final EncodingAttributes attrs = ConfigOptions.encodingattributes;
	protected final AtomicBoolean status;
	protected final File input, output;

	protected Converter(boolean async, File input, File output) {
		this.input = input;
		this.output = output;
		if (async) {
			status = new AtomicBoolean(false);
			new Thread(this).start();
		} else {
			status = null;
			run();
		}
	}

	@Override
	public void run() {
		try {
			super.encode(new MultimediaObject(input), output, attrs);
		} catch (IllegalArgumentException | EncoderException e) {
		}
		if (status != null) {
			status.set(true);
		}
	}
}
