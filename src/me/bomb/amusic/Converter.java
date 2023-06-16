package me.bomb.amusic;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import ws.schild.jave.ConversionOutputAnalyzer;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.utils.RBufferedReader;

final class Converter implements Runnable {
	protected final AtomicBoolean status;
	protected final File input, output;
	
	private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);

	protected Converter(boolean async, File input, File output) {
		this.input = input;
		this.output = output.getAbsoluteFile();
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
			ProcessWrapper ffmpeg = new ProcessWrapper();

			try {
				ffmpeg.execute(true, true, input.getAbsolutePath(), output.getAbsolutePath());
			} catch (IOException e) {
				ffmpeg.close();
				return;
			}

			try {
				String lastWarning = null;

				String line;
				ConversionOutputAnalyzer outputAnalyzer = new ConversionOutputAnalyzer();
				RBufferedReader reader = new RBufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					outputAnalyzer.analyzeNewLine(line);
				}
				if (outputAnalyzer.getLastWarning() != null) {
					if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
						return;
					}
				}
				int exitCode = ffmpeg.getProcessExitCode();
				if (exitCode != 0) {
					return;
				}
			} catch (IOException e) {
				return;
			} finally {
				if (ffmpeg != null) {
					ffmpeg.destroy();
				}
				ffmpeg = null;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		if (status != null) {
			status.set(true);
		}
	}

}
