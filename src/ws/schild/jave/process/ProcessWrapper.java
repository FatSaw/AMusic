/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 *
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ws.schild.jave.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.bomb.amusic.ConfigOptions;

public class ProcessWrapper implements AutoCloseable {

	private Process ffmpeg = null;

	private Thread ffmpegKiller = null;

	private InputStream inputStream = null;

	private OutputStream outputStream = null;

	private InputStream errorStream = null;

	public void execute(boolean destroyOnRuntimeShutdown, boolean openIOStreams, String inputfile, String outputfile) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		StringBuffer sb = new StringBuffer(ConfigOptions.fmpegbinarypath);
		sb.append(" -i ");
		sb.append(inputfile);
		sb.append(ConfigOptions.binarywithargs);
		if (outputfile != null) {
			sb.append(" -y ");
			sb.append(outputfile);
		}
		ffmpeg = runtime.exec(sb.toString());

		if (destroyOnRuntimeShutdown) {
			ffmpegKiller = new Thread() {
				private final Process affmpeg = ffmpeg;
				@Override
				public void run() {
					affmpeg.destroy();
				}
			};
			runtime.addShutdownHook(ffmpegKiller);
		}

		if (openIOStreams) {
			inputStream = ffmpeg.getInputStream();
			outputStream = ffmpeg.getOutputStream();
			errorStream = ffmpeg.getErrorStream();
		}
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public InputStream getErrorStream() {
		return errorStream;
	}

	public void destroy() {
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
	}
	
	public int getProcessExitCode() {
		// Make sure it's terminated
		try {
			ffmpeg.waitFor();
		} catch (InterruptedException ex) {
		}
		return ffmpeg.exitValue();
	}

	@Override
	public void close() {
		destroy();
	}
}
