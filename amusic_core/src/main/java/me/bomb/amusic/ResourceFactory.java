package me.bomb.amusic;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import me.bomb.amusic.resourceserver.ResourceManager;
import me.bomb.amusic.source.SoundSource;

public final class ResourceFactory implements Runnable {
	
	private final String id, name;
	private final UUID[] targets;
	private final String host;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final ResourcePacker resourcepacker;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	private final String[] soundnames;
	private final short[] soundlengths;
	private final byte[] sha1;
	private final File resourcefile;

	private ResourceFactory(SoundSource source, String host, File musicdir, File packeddir, int maxpacksize, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID[] targets, String id, boolean update) throws FileNotFoundException {
		this.id = id;
		this.targets = targets;
		this.host = host;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		id = toBase64(id); //now name is safe for files
		String[] asongnames = null;
		short[] asonglengths = null;
		byte[] asha1 = null;
		this.resourcefile = new File(packeddir, id.concat(".zip"));
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
		musicdir = new File(musicdir, this.name);
		File sourcearchive = new File(musicdir, this.name.concat(".zip"));
		if (!ok) {
			if (!musicdir.exists()) {
				throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
			}
			this.resourcepacker = new ResourcePacker(source, maxpacksize, this.name, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
			this.soundnames = null;
			this.soundlengths = null;
			this.sha1 = null;
		} else {
			this.resourcepacker = null;
			this.soundnames = asongnames;
			this.soundlengths = asonglengths;
			this.sha1 = asha1;
			new Thread(this).start();
		}
	}

	private ResourceFactory(SoundSource source, String host, File musicdir, File packeddir, int maxpacksize, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, String id) throws FileNotFoundException {
		this.host = host;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.id = id;
		this.targets = null;
		this.packsender = null;
		id = toBase64(id); //now name is safe for files
		String name = null;
		if (data.containsPlaylist(this.id)) {
			DataEntry options = data.getPlaylist(this.id);
			name = options.name;
			File resourcefile = new File(packeddir, id);
			if (resourcefile != null && resourcefile.exists()) {
				resourcefile.delete();
				data.removePlaylist(this.id);
			}
		} else {
			name = filterName(this.id);
		}
		this.name = name;
		musicdir = new File(musicdir, this.name);
		File sourcearchive = new File(musicdir, this.name.concat(".zip"));
		if (!musicdir.exists()) {
			throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
		}
		this.resourcefile = new File(packeddir, id.concat(".zip"));
		this.resourcepacker = new ResourcePacker(source, maxpacksize, this.name, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
		this.soundnames = null;
		this.soundlengths = null;
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

	public static boolean load(SoundSource source, ConfigOptions configoptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID[] targets, String name, boolean update) throws FileNotFoundException {
		if(name==null || name.isEmpty()) {
			return false;
		}
		boolean processpack = configoptions.processpack;
		if ((targets == null || targets.length == 0) && processpack) {
			ResourceFactory resourcepacked = new ResourceFactory(source, configoptions.host, configoptions.musicdir, configoptions.packeddir, configoptions.maxpacksize, data, resourcemanager, positiontracker, name);
			if (resourcepacked.resourcepacker != null && !resourcepacked.resourcepacker.isAlive()) {
				resourcepacked.resourcepacker.start();
				return true;
			}
			return false;
		}
		update &= processpack;
		ResourceFactory resourcepacked = new ResourceFactory(source, configoptions.host, configoptions.musicdir, configoptions.packeddir, configoptions.maxpacksize, data, resourcemanager, positiontracker, packsender, targets, name, update);
		if (resourcepacked.resourcepacker == null) {
			positiontracker.removeAll(targets);
			return true;
		} else if(processpack) {
			resourcepacked.resourcepacker.start();
			positiontracker.removeAll(targets);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		String[] soundnames;
		short[] soundlengths;
		if(this.resourcepacker != null) {
			soundnames = resourcepacker.soundnames;
			soundlengths = resourcepacker.soundlengths;
			if(resourcefile.exists()) {
				data.setPlaylist(id, soundnames, soundlengths, (int)resourcefile.length(), this.name, resourcepacker.sha1);
				data.save();
			} else {
				data.removePlaylist(id);
				data.save();
				return;
			}
		} else {
			soundnames = this.soundnames;
			soundlengths = this.soundlengths;
		}
		if(targets == null || packsender == null) {
			return;
		}
		int soundssize = soundnames.length;
		if (soundssize != soundlengths.length) {
			return;
		}
		for(UUID target : targets) {
			StringBuilder sb = new StringBuilder("http://");
			sb.append(host);
			sb.append("/");
			sb.append(resourcemanager.add(target, this.resourcefile));
			sb.append(".zip");
			packsender.send(target, sb.toString(), this.sha1 == null ? resourcepacker.sha1 : this.sha1);
			SoundInfo[] soundinfos = new SoundInfo[soundssize];
			for(int i=0;i<soundssize;++i) {
				soundinfos[i] = new SoundInfo(soundnames[i], soundlengths[i]);
			}
			positiontracker.setPlaylistInfo(target, this.id, soundinfos);
		}
	}
}
