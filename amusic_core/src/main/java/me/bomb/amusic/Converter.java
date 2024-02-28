package me.bomb.amusic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Converter implements Runnable {
	
	private static final Pattern SUCCESS_PATTERN, PROGRESS_INFO_PATTERN;
	private static final String fmpegbinarypath;
	
	private final AtomicBoolean status;
	protected final File input, output;
	
	//private final List<String> unhandledMessages = new LinkedList<>();
	
	private final int bitrate, samplingrate;
	private final byte channels;
	
	static {
		SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);
		PROGRESS_INFO_PATTERN = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);
		String os = System.getProperty("os.name").toLowerCase();
		fmpegbinarypath = new File("plugins/AMusic/", "ffmpeg".concat(os.contains("windows") ? ".exe" : os.contains("mac") ? "-osx" : "")).getAbsolutePath();
	}

	protected Converter(boolean async,int bitrate, byte channels, int samplingrate ,File input, File output) {
		this.input = input;
		this.output = output.getAbsoluteFile();
		this.bitrate = bitrate;
		this.channels = channels;
		this.samplingrate = samplingrate;
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
		StringBuilder sb = new StringBuilder(fmpegbinarypath);
		sb.append(" -i ");
		sb.append(input.getAbsolutePath());
		sb.append(" -acodec libvorbis -ab ");
		sb.append(Integer.toString(bitrate));
		sb.append(" -ac ");
		sb.append(Byte.toString(channels));
		sb.append(" -ar ");
		sb.append(Integer.toString(samplingrate));
		sb.append(" -f ogg -y ");
		sb.append(output.getAbsolutePath());
		String cmd = sb.toString();
		Runtime runtime = Runtime.getRuntime();
		Process ffmpeg = null;
		try {
			ffmpeg = runtime.exec(cmd);
		} catch (SecurityException e) {
			
			return;
		} catch(IOException e) {
			return;
		}
		
		if(ffmpeg == null) {
			return;
		}
		
		ProcessKiller ffmpegKiller = new ProcessKiller(ffmpeg);
		runtime.addShutdownHook(ffmpegKiller);
		
		InputStream errorStream = ffmpeg.getErrorStream();
		
		if(errorStream==null) {
			ffmpeg.destroy();
			return;
		}
		
		try {
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
			byte step = 0;
			String lastWarning = null;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("Press [q]")) {
					switch (step) {
					case 0: {
						if (line.startsWith("Input #0")) {
							step = 1;
						}
					}
						break;
					case 1: {
						if (line.startsWith("Stream mapping:") || line.startsWith("Output #0")) {
							step = 2;
						} else if (!line.startsWith("  ")) {
							//unhandledMessages.add(line);
						}
					}
						break;
					case 2: {
						if (line.startsWith("Output #0") || line.startsWith("Stream mapping:")) {
							step = 3;
						} else if (!line.startsWith("  ")) {
							//unhandledMessages.add(line);
						}
					}
						break;
					case 3: {
						if (line.startsWith("  ")) {
							
						} else if (line.startsWith("video:")) {
							step = 4;
						} else if (line.startsWith("frame=")) {
							
						} else if (line.startsWith("size=")) {
							
						} else if (line.endsWith("Queue input is backward in time") || line.contains("Application provided invalid, non monotonically increasing dts to muxer in stream")) {
							
						} else {
							//unhandledMessages.add(line);
						}
					}
					}
					if (line.startsWith("frame=") || line.startsWith("size=")) {
						try {
							line = line.trim();
							if (line.length() > 0) {
								HashMap<String, String> table = null;
								Matcher m = PROGRESS_INFO_PATTERN.matcher(line);
								while (m.find()) {
									if (table == null) {
										table = new HashMap<>();
									}
									String key = m.group(1);
									String value = m.group(2);
									table.put(key, value);
								}
								if (table == null) {
									lastWarning = line;
								} else {
									lastWarning = null;
								}
							}
						} catch (Exception ex) {
						}
					}
				}
			}
			if (lastWarning != null) {
				if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
					return;
				}
			}
			try {
				ffmpeg.waitFor();
			} catch (InterruptedException ex) {
			}
			if (ffmpeg.exitValue() != 0) {
				return;
			}
		} catch (IOException e) {
			return;
		} finally {
			ffmpeg.destroy();
			ffmpeg = null;
		}
		
		if (status != null) {
			status.set(true);
		}
	}
	
	protected boolean finished() {
		return status == null || status.get();
	}

	private final class ProcessKiller extends Thread {
		private final Process process;

		private ProcessKiller(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			process.destroy();
		}
	}
}
