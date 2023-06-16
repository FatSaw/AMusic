package me.bomb.amusic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Converter implements Runnable {
	protected final AtomicBoolean status;
	protected final File input, output;

	private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+subtitle\\:\\S+\\s+global headers\\:\\S+.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern PROGRESS_INFO_PATTERN = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

	private int step = 0;
	private String lastWarning = null;
	private final List<String> unhandledMessages = new LinkedList<>();

	private Process ffmpeg = null;

	private Thread ffmpegKiller = null;

	private InputStream inputStream = null;

	private OutputStream outputStream = null;

	private InputStream errorStream = null;

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
			try {

				Runtime runtime = Runtime.getRuntime();
				StringBuffer sb = new StringBuffer(ConfigOptions.fmpegbinarypath);
				sb.append(" -i ");
				sb.append(input.getAbsolutePath());
				sb.append(ConfigOptions.binarywithargs);
				sb.append(" -y ");
				sb.append(output.getAbsolutePath());
				ffmpeg = runtime.exec(sb.toString());

				ffmpegKiller = new ProcessKiller(ffmpeg);
				runtime.addShutdownHook(ffmpegKiller);

				inputStream = ffmpeg.getInputStream();
				outputStream = ffmpeg.getOutputStream();
				errorStream = ffmpeg.getErrorStream();

			} catch (IOException e) {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (Throwable t) {
					}
					inputStream = null;
				}

				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (Throwable t) {
					}
					outputStream = null;
				}

				if (errorStream != null) {
					try {
						errorStream.close();
					} catch (Throwable t) {
					}
					errorStream = null;
				}

				if (ffmpeg != null) {
					ffmpeg.destroy();
					ffmpeg = null;
				}

				if (ffmpegKiller != null) {
					Runtime runtime = Runtime.getRuntime();
					runtime.removeShutdownHook(ffmpegKiller);
					ffmpegKiller = null;
				}
				return;
			}

			try {
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
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
								unhandledMessages.add(line);
							}
						}
							break;
						case 2: {
							if (line.startsWith("Output #0") || line.startsWith("Stream mapping:")) {
								step = 3;
							} else if (!line.startsWith("  ")) {
								unhandledMessages.add(line);
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
								unhandledMessages.add(line);
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
