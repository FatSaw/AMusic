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
package ws.schild.jave.process.ffmpeg;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.bomb.amusic.ConfigOptions;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;

/**
 * The default ffmpeg executable locator, which exports on disk the ffmpeg executable bundled with
 * the library distributions. It should work both for windows and many linux distributions. If it
 * doesn't, try compiling your own ffmpeg executable and plug it in JAVE with a custom {@link
 * FFMPEGProcess}
 *
 * @author Carlo Pelliccia
 */
public class DefaultFFMPEGLocator implements ProcessLocator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFFMPEGLocator.class);

  /** The ffmpeg executable file path. */
  private final String path;

  /** It builds the default FFMPEGLocator, exporting the ffmpeg executable on a temp file. */
  public DefaultFFMPEGLocator() {
    String os = System.getProperty("os.name").toLowerCase();
    boolean isWindows = os.contains("windows");
    boolean isMac = os.contains("mac");
    LOG.debug("Os name is <{}> isWindows: {} isMac: {}", os, isWindows, isMac);

    File ffmpegFile =  new File(ConfigOptions.ffmpegbinary.toString());
    LOG.debug("Executable path: {}", ffmpegFile.getAbsolutePath());

    path = ffmpegFile.getAbsolutePath();
    if (ffmpegFile.exists())
    {
        LOG.debug("ffmpeg executable found: {}", path);
    }
    else
    {
        LOG.error("ffmpeg executable NOT found: {}", path);
    }
  }

  @Override
  public String getExecutablePath() {
    return path;
  }

  @Override
  public ProcessWrapper createExecutor() {
    return new FFMPEGProcess(getExecutablePath());
  }
}
