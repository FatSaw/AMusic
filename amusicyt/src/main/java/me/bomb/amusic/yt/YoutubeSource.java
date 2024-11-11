package me.bomb.amusic.yt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoStreamDownload;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;

import me.bomb.amusic.source.SoundSource;
import me.bomb.amusic.source.SourceEntry;

public final class YoutubeSource extends SoundSource<YoutubeDownloader> {

	private final YoutubeDownloader downloader;
	private final Runtime runtime;
	private final File tempdirectory, fmpegbinary;
	private final int maxsoundsize, bitrate, samplingrate;
	private final byte channels;
	private final Encoder base64encoder;

	protected YoutubeSource(File tempdirectory, Runtime runtime, int maxsoundsize, File fmpegbinary, int bitrate, byte channels, int samplingrate) {
		this.downloader = new YoutubeDownloader();
		this.runtime = runtime;
		this.tempdirectory = tempdirectory;
		this.maxsoundsize = maxsoundsize;
		this.fmpegbinary = fmpegbinary;
		this.bitrate = bitrate;
		this.samplingrate = samplingrate;
		this.channels = channels;
		this.base64encoder = Base64.getUrlEncoder();
	}
	
	private String toBase64(String name) {
		return new String(base64encoder.encode(name.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
	}

	@Override
	public SourceEntry get(String playlistId) {
		if(playlistId.length() > 17) {
			RequestPlaylistInfo requestplaylist = new RequestPlaylistInfo(playlistId);
			Response<PlaylistInfo> responseplaylist = downloader.getPlaylistInfo(requestplaylist);
			PlaylistInfo playlistInfo = responseplaylist.data();
			List<PlaylistVideoDetails> videos = playlistInfo.videos(), playable = new ArrayList<>();
			{
				int i = videos.size();
				while (--i > -1) {
					PlaylistVideoDetails videodetails = videos.get(i);
					if(videodetails.isPlayable()) {
						playable.add(videodetails);
					}
				}
			}
			String playlistIdbase64 = toBase64(playlistId);
			File tempdirectory = new File(this.tempdirectory, playlistIdbase64);
			if(tempdirectory.isDirectory()) {
				File[] files = tempdirectory.listFiles();
				int i = files.length;
				while(--i > -1) {
					if(files[i].isFile()) {
						files[i].delete();
					}
				}
				tempdirectory.delete();
			}
			tempdirectory.mkdir();
			
			
			
			
			int i = playable.size();
			String[] names = new String[i];
			short[] lengths = new short[i];
			byte[][] data = new byte[i][];
			AtomicBoolean[] finished = new AtomicBoolean[i];
			boolean[] success = new boolean[i];
			
			String[] args = new String[] { fmpegbinary.getAbsolutePath(), "-i", null, "-strict", "-2", "-acodec", "vorbis",
					"-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate),
					"-f", "ogg", "-vn", "-y", "pipe:1" };

			while (--i > -1) {
				PlaylistVideoDetails videodetails = playable.get(i);
				names[i] = videodetails.title();
				
				final String videoId = videodetails.videoId();
				
				RequestVideoInfo request = new RequestVideoInfo(videoId).callback(new YoutubeCallback<VideoInfo>() {
					@Override
					public void onFinished(VideoInfo videoInfo) {
						//Bukkit.broadcastMessage("DEBUG: Found INFO");
					}

					@Override
					public void onError(Throwable throwable) {
						//Bukkit.broadcastMessage("DEBUG: Error: " + throwable.getMessage());
					}
				}).async();
				Response<VideoInfo> response = downloader.getVideoInfo(request);
				VideoInfo video = response.data();
				//Bukkit.broadcastMessage("DEBUG: Found video: " + names[i]);

				AudioFormat bestaudio = video.bestAudioFormat();
				//Bukkit.broadcastMessage("Audio: " + bestaudio == null ? "no" : " Quality: " + bestaudio.audioQuality().name());
				
				//Bukkit.broadcastMessage("DEBUG: Download started!");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				RequestVideoStreamDownload download = new RequestVideoStreamDownload(bestaudio, baos);
				downloader.downloadVideoStream(download);
				byte[] sound = baos.toByteArray();
				File tempfile = new File(tempdirectory, toBase64(names[i]));
				{
					try {
						FileOutputStream fos = new FileOutputStream(tempfile, false);
						fos.write(sound);
						fos.close();
					} catch (IOException e) {
					}
					args[2] = null;
					args[2] = tempfile.getAbsolutePath();
				}
				
				//Bukkit.broadcastMessage("DEBUG: Download finished (size: " + baos.size() + ")!");
				
				AtomicBoolean finishede = new AtomicBoolean(false);
				finished[i] = finishede;
				final Process ffmpeg;
				try {
					//ffmpeg = new ProcessBuilder(args).redirectInput(Redirect.PIPE).redirectOutput(Redirect.PIPE).start();
					ffmpeg = runtime.exec(args);
				} catch (IOException | SecurityException e) {
					//Bukkit.broadcastMessage("DEBUG: FFMpeg failed to start: " + e.getMessage());
					continue;
				}
				if (ffmpeg == null) {
					//Bukkit.broadcastMessage("DEBUG: FFMpeg failed to start!");
					continue;
				}

				/*new Thread() {
					public void run() {
						BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
						while(ffmpeg.isAlive()) {
							try {
								while(reader.ready()) {
								     String line = reader.readLine();
								     Bukkit.broadcastMessage("DEBUGFFMPEG: " + line);
								}
							} catch (IOException e) {
							}
						}
					}
				}.start();*/

				InputStream is = null;
				try {
					//Bukkit.broadcastMessage("DEBUG: FFMpeg started!");
					/*OutputStream os = ffmpeg.getOutputStream();
					try {
						//int e = sound.length / 4096;
						//int j = 0, k = 0;
						//while (j < e) {
						//	++j;
						//	os.write(sound, k, 4096);
						//	k+=4096;
						//	Bukkit.broadcastMessage("DEBUG: FFMpeg write()");
						//}
						//os.write(sound, k, sound.length-k);
						//Bukkit.broadcastMessage("DEBUG: FFMpeg LAST write()");
						os.write(sound, 0, sound.length);
						Bukkit.broadcastMessage("DEBUG: FFMpeg write()");
						//os.flush();
						//Bukkit.broadcastMessage("DEBUG: FFMpeg flush()");
					} catch (IOException e) {
						Bukkit.broadcastMessage("DEBUG: FFMpeg input sound error!");
						continue;
					}*/

					//Bukkit.broadcastMessage("DEBUG: FFMpeg input done!");
					is = ffmpeg.getInputStream();
					ByteArrayOutputStream bos = new ByteArrayOutputStream(maxsoundsize);
					int b;
					while ((b = is.read()) != -1) {
						bos.write(b);
					}
					byte[] resource = bos.toByteArray();
					lengths[i] = calculateDuration(resource);
					data[i] = resource;
					success[i] = true;

					//Bukkit.broadcastMessage("DEBUG: Success!");
				} catch (IOException e) {
					//Bukkit.broadcastMessage("DEBUG:Exception: " + e.getMessage());
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
						}
					}
				}
				finishede.set(true);
				//Bukkit.broadcastMessage("DEBUG: Finish!");
				tempfile.delete();
			}
			tempdirectory.delete();
			SourceEntry source = new SourceEntry(names, lengths, data, finished, success);
			return source;
		}
		
		
		
		String playlistIdbase64 = toBase64(playlistId);
		File tempdirectory = new File(this.tempdirectory, playlistIdbase64);
		if(tempdirectory.isDirectory()) {
			File[] files = tempdirectory.listFiles();
			int i = files.length;
			while(--i > -1) {
				if(files[i].isFile()) {
					files[i].delete();
				}
			}
			tempdirectory.delete();
		}
		tempdirectory.mkdir();
		
		
		
		
		int i = 1;
		String[] names = new String[i];
		short[] lengths = new short[i];
		byte[][] data = new byte[i][];
		AtomicBoolean[] finished = new AtomicBoolean[i];
		boolean[] success = new boolean[i];
		
		--i;
		
		String[] args = new String[] { fmpegbinary.getAbsolutePath(), "-i", null, "-strict", "-2", "-acodec", "vorbis",
				"-ab", Integer.toString(bitrate), "-ac", Byte.toString(channels), "-ar", Integer.toString(samplingrate),
				"-f", "ogg", "-vn", "-y", "pipe:1" };
		
		
		
		final String videoId = playlistId;
		
		RequestVideoInfo request = new RequestVideoInfo(videoId).callback(new YoutubeCallback<VideoInfo>() {
			@Override
			public void onFinished(VideoInfo videoInfo) {
				//Bukkit.broadcastMessage("DEBUG: Found INFO");
			}

			@Override
			public void onError(Throwable throwable) {
				//Bukkit.broadcastMessage("DEBUG: Error: " + throwable.getMessage());
			}
		}).async();
		Response<VideoInfo> response = downloader.getVideoInfo(request);
		VideoInfo video = response.data();
		//Bukkit.broadcastMessage("DEBUG: Found video: " + names[i]);

		names[i] = video.details().title();
		AudioFormat bestaudio = video.bestAudioFormat();
		//Bukkit.broadcastMessage("Audio: " + bestaudio == null ? "no" : " Quality: " + bestaudio.audioQuality().name());
		
		//Bukkit.broadcastMessage("DEBUG: Download started!");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		RequestVideoStreamDownload download = new RequestVideoStreamDownload(bestaudio, baos);
		downloader.downloadVideoStream(download);
		byte[] sound = baos.toByteArray();
		File tempfile = new File(tempdirectory, toBase64(names[i]));
		{
			try {
				FileOutputStream fos = new FileOutputStream(tempfile, false);
				fos.write(sound);
				fos.close();
			} catch (IOException e) {
			}
			args[2] = null;
			args[2] = tempfile.getAbsolutePath();
		}
		
		//Bukkit.broadcastMessage("DEBUG: Download finished (size: " + baos.size() + ")!");
		
		AtomicBoolean finishede = new AtomicBoolean(false);
		finished[i] = finishede;
		final Process ffmpeg;
		try {
			//ffmpeg = new ProcessBuilder(args).redirectInput(Redirect.PIPE).redirectOutput(Redirect.PIPE).start();
			ffmpeg = runtime.exec(args);
		} catch (IOException | SecurityException e) {
			//Bukkit.broadcastMessage("DEBUG: FFMpeg failed to start: " + e.getMessage());
			return null;
		}
		if (ffmpeg == null) {
			//Bukkit.broadcastMessage("DEBUG: FFMpeg failed to start!");
			return null;
		}

		/*new Thread() {
			public void run() {
				BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
				while(ffmpeg.isAlive()) {
					try {
						while(reader.ready()) {
						     String line = reader.readLine();
						     Bukkit.broadcastMessage("DEBUGFFMPEG: " + line);
						}
					} catch (IOException e) {
					}
				}
			}
		}.start();*/

		InputStream is = null;
		try {
			//Bukkit.broadcastMessage("DEBUG: FFMpeg started!");
			/*OutputStream os = ffmpeg.getOutputStream();
			try {
				//int e = sound.length / 4096;
				//int j = 0, k = 0;
				//while (j < e) {
				//	++j;
				//	os.write(sound, k, 4096);
				//	k+=4096;
				//	Bukkit.broadcastMessage("DEBUG: FFMpeg write()");
				//}
				//os.write(sound, k, sound.length-k);
				//Bukkit.broadcastMessage("DEBUG: FFMpeg LAST write()");
				os.write(sound, 0, sound.length);
				Bukkit.broadcastMessage("DEBUG: FFMpeg write()");
				//os.flush();
				//Bukkit.broadcastMessage("DEBUG: FFMpeg flush()");
			} catch (IOException e) {
				Bukkit.broadcastMessage("DEBUG: FFMpeg input sound error!");
				continue;
			}*/

			//Bukkit.broadcastMessage("DEBUG: FFMpeg input done!");
			is = ffmpeg.getInputStream();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(maxsoundsize);
			int b;
			while ((b = is.read()) != -1) {
				bos.write(b);
			}
			byte[] resource = bos.toByteArray();
			lengths[i] = calculateDuration(resource);
			data[i] = resource;
			success[i] = true;

			//Bukkit.broadcastMessage("DEBUG: Success!");
		} catch (IOException e) {
			//Bukkit.broadcastMessage("DEBUG:Exception: " + e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		finishede.set(true);
		//Bukkit.broadcastMessage("DEBUG: Finish!");
		tempfile.delete();
		tempdirectory.delete();
		
		SourceEntry source = new SourceEntry(names, lengths, data, finished, success);
		return source;
	}

	@Override
	public boolean exists(String playlistId) {
		if(playlistId.length() > 17) {
			RequestPlaylistInfo requestplaylist = new RequestPlaylistInfo(playlistId);
			Response<PlaylistInfo> responseplaylist = downloader.getPlaylistInfo(requestplaylist);
			PlaylistInfo playlistInfo = responseplaylist.data();
			List<PlaylistVideoDetails> videos = playlistInfo.videos();
			{
				int i = videos.size();
				while (--i > -1) {
					PlaylistVideoDetails videodetails = videos.get(i);
					if(videodetails.isPlayable()) {
						return true;
					}
				}
			}
			return false;
		}
		RequestVideoInfo request = new RequestVideoInfo(playlistId).callback(new YoutubeCallback<VideoInfo>() {
			@Override
			public void onFinished(VideoInfo videoInfo) {
				//Bukkit.broadcastMessage("DEBUG: Found INFO");
			}

			@Override
			public void onError(Throwable throwable) {
				//Bukkit.broadcastMessage("DEBUG: Error: " + throwable.getMessage());
			}
		}).async();
		Response<VideoInfo> response = downloader.getVideoInfo(request);
		return response.data() != null;
	}

	@Override
	public YoutubeDownloader getSource() {
		return downloader;
	}

}
