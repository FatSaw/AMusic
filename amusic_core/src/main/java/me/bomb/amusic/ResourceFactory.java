package me.bomb.amusic;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourceFactory implements Runnable {
	
	private final String id, name;
	private final UUID target;
	private final ConfigOptions configoptions;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final ResourcePacker resourcepacker;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	private final List<String> soundnames;
	private final List<Short> soundlengths;
	private final byte[] sha1;
	private final File resourcefile;

	private ResourceFactory(ConfigOptions configoptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String id, boolean update) throws FileNotFoundException {
		this.id = id;
		this.target = target;
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		id = toBase64(id); //now name is safe for files
		List<String> asongnames = null;
		List<Short> asonglengths = null;
		byte[] asha1 = null;
		this.resourcefile = new File(configoptions.packeddir, id.concat(".zip"));
		boolean ok = false;
		String name = null;
		if (data.containsPlaylist(this.id)) {
			DataEntry options = data.getPlaylist(this.id);
			name = options.name;
			if (resourcefile != null && resourcefile.exists()) {
				if (update) {
					resourcefile.delete();
					data.removePlaylist(this.id);
				} else if (resourcemanager.isCached(resourcefile.toPath()) || options.check(resourcefile)) {
					asongnames = options.sounds;
					asonglengths = options.length;
					asha1 = options.sha1;
					ok = true;
				}
			}
		} else {
			name = filterName(this.id);
		}
		this.name = name;
		File musicdir = new File(configoptions.musicdir, this.name);
		File tempdir = new File(configoptions.tempdir, id);
		File sourcearchive = new File(configoptions.musicdir, this.name.concat(".zip"));
		if (!ok) {
			if (!musicdir.exists()) {
				throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
			}
			this.resourcepacker = new ResourcePacker(configoptions.useconverter, configoptions.bitrate, configoptions.channels, configoptions.samplingrate, configoptions.encodetracksasynchronly, configoptions.maxpacksize, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
			this.soundnames = resourcepacker.soundnames;
			this.soundlengths = resourcepacker.soundlengths;
			this.sha1 = null;
		} else {
			this.resourcepacker = null;
			this.soundnames = asongnames;
			this.soundlengths = asonglengths;
			this.sha1 = asha1;
			new Thread(this).start();
		}
	}

	private ResourceFactory(ConfigOptions configoptions,Data data, ResourceManager resourcemanager, PositionTracker positiontracker, String id) throws FileNotFoundException {
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.id = id;
		this.target = null;
		this.packsender = null;
		id = toBase64(id); //now name is safe for files
		String name = null;
		if (data.containsPlaylist(this.id)) {
			DataEntry options = data.getPlaylist(this.id);
			name = options.name;
			File resourcefile = new File(configoptions.packeddir, id);
			if (resourcefile != null && resourcefile.exists()) {
				resourcefile.delete();
				data.removePlaylist(this.id);
			}
		} else {
			name = filterName(this.id);
		}
		this.name = name;
		File musicdir = new File(configoptions.musicdir, this.name);
		File tempdir = new File(configoptions.tempdir, id);
		File sourcearchive = new File(configoptions.musicdir, this.name.concat(".zip"));
		if (!musicdir.exists()) {
			throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
		}
		this.resourcefile = new File(configoptions.packeddir, id.concat(".zip"));
		this.resourcepacker = new ResourcePacker(configoptions.useconverter, configoptions.bitrate, configoptions.channels, configoptions.samplingrate, configoptions.encodetracksasynchronly, configoptions.maxpacksize, configoptions.maxmusicfilesize, configoptions.ffmpegbinary, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
		soundnames = resourcepacker.soundnames;
		soundlengths = resourcepacker.soundlengths;
		this.sha1 = resourcepacker.sha1;
	}
	
	public static String toBase64(String name) {
		return new String(Base64.getUrlEncoder().encode(name.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
	}
	
	public static String filterName(String name) {
		char[] chars = name.toCharArray();
		int finalcount = 0;
		int i = chars.length;
		while(--i > -1) {
			char c = chars[i];
			//if(c == '/' || c == '\\' || c == ':' || c == '<' || c == '>' || c == '*' || c == '?' || c == '|' || c == '\"' || c == '\0' || (c > 0 && c < 32)) { // who use windows for servers
			if(c == '/' || c == '\0') { //unix
				chars[i] = '\0';
			} else {
				++finalcount;
			}
		}
		char[] filtered = new char[finalcount];
		int j = 0;
		while(++i < chars.length && j < finalcount) {
			char c = chars[i];
			if(c != '\0') {
				filtered[j] = c;
				++j;
			}
		}
		return new String(filtered);
	}

	public static boolean load(ConfigOptions configoptions,Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String name, boolean update) throws FileNotFoundException {
		if(name==null || name.isEmpty()) {
			return false;
		}
		boolean processpack = configoptions.processpack;
		if (target == null && processpack) {
			ResourceFactory resourcepacked = new ResourceFactory(configoptions, data, resourcemanager, positiontracker, name);
			if (resourcepacked.resourcepacker != null && !resourcepacked.resourcepacker.isAlive()) {
				resourcepacked.resourcepacker.start();
				return true;
			}
			return false;
		}
		update &= processpack;
		ResourceFactory resourcepacked = new ResourceFactory(configoptions, data, resourcemanager, positiontracker, packsender, target, name, update);
		if (resourcepacked.resourcepacker == null) {
			positiontracker.remove(target);
			return true;
		} else if(processpack) {
			resourcepacked.resourcepacker.start();
			positiontracker.remove(target);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		if(this.resourcepacker != null) {
			if(resourcefile.exists()) {
				data.setPlaylist(id, soundnames, soundlengths, (int)resourcefile.length(), this.name, resourcepacker.sha1);
				data.save();
			} else {
				data.removePlaylist(id);
				data.save();
				return;
			}
		}
		if(target == null || packsender == null) {
			return;
		}
		int soundssize = soundnames.size();
		if (soundssize != soundlengths.size()) {
			return;
		}
		StringBuilder sb = new StringBuilder("http://");
		sb.append(configoptions.host);
		sb.append("/");
		sb.append(resourcemanager.add(target, this.resourcefile));
		sb.append(".zip");
		packsender.send(target, sb.toString(), this.sha1 == null ? resourcepacker.sha1 : this.sha1);
		ArrayList<SoundInfo> soundinfos = new ArrayList<SoundInfo>(soundssize);
		for(int i=0;i<soundssize;++i) {
			soundinfos.add(new SoundInfo(soundnames.get(i), soundlengths.get(i)));
		}
		positiontracker.setPlaylistInfo(target, this.id, soundinfos);
	}
}
