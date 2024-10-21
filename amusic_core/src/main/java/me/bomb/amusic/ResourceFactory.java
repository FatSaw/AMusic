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
	private final SoundInfo[] sounds;
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
		SoundInfo[] sounds = null;
		byte[] asha1 = null;
		this.resourcefile = new File(packeddir, id.concat(".zip"));
		boolean ok = false, updateremove = false;
		String name = null;
		if (data.containsPlaylist(this.id)) {
			DataEntry options = data.getPlaylist(this.id);
			name = options.name;
			if (update) {
				if (resourcefile != null && resourcefile.exists()) {
					resourcefile.delete();
				}
				data.removePlaylist(this.id);
				data.save();
				updateremove = true;
			} else if (resourcefile != null && resourcefile.exists() && (resourcemanager.isCached(resourcefile.toPath()) || options.check(resourcefile))) {
				sounds = options.sounds;
				asha1 = options.sha1;
				ok = true;
			}
		} else {
			name = filterName(this.id);
		}
		this.name = name;
		musicdir = new File(musicdir, this.name);
		File sourcearchive = new File(musicdir, this.name.concat(".zip"));
		if (!ok) {
			if(!source.exists(this.name)) {
				if(updateremove) {
					this.resourcepacker = null;
					this.sounds = null;
					this.sha1 = null;
					return;
				}
				throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
			}
			this.resourcepacker = new ResourcePacker(source, maxpacksize, this.name, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
			this.sounds = null;
			this.sha1 = null;
		} else {
			this.resourcepacker = null;
			this.sounds = sounds;
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
		this.resourcefile = new File(packeddir, id.concat(".zip"));
		boolean updateremove = false;
		String name = null;
		if (data.containsPlaylist(this.id)) {
			DataEntry options = data.getPlaylist(this.id);
			name = options.name;
			if (resourcefile != null && resourcefile.exists()) {
				resourcefile.delete();
			}
			data.removePlaylist(this.id);
			data.save();
			updateremove = true;
		} else {
			name = filterName(this.id);
		}
		this.name = name;
		musicdir = new File(musicdir, this.name);
		File sourcearchive = new File(musicdir, this.name.concat(".zip"));
		if(!source.exists(this.name)) {
			if(updateremove) {
				this.resourcepacker = null;
				this.sounds = null;
				this.sha1 = null;
				return;
			}
			throw new FileNotFoundException("No music directory: ".concat(musicdir.getPath()));
		}
		this.resourcepacker = new ResourcePacker(source, maxpacksize, this.name, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
		this.sounds = null;
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
		if (targets == null && processpack) {
			ResourceFactory resourcepacked = new ResourceFactory(source, configoptions.host, configoptions.musicdir, configoptions.packeddir, configoptions.maxpacksize, data, resourcemanager, positiontracker, name);
			if (resourcepacked.resourcepacker != null && !resourcepacked.resourcepacker.isAlive()) {
				resourcepacked.resourcepacker.start();
			}
			return true;
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
		SoundInfo[] sounds;
		if(this.resourcepacker != null) {
			sounds = resourcepacker.sounds;
			if(resourcefile.exists()) {
				data.setPlaylist(id, sounds, (int)resourcefile.length(), this.name, resourcepacker.sha1);
				data.save();
			} else {
				data.removePlaylist(id);
				data.save();
				return;
			}
		} else {
			sounds = this.sounds;
		}
		if(targets == null || packsender == null) {
			return;
		}

		final String httphost = "http://".concat(host).concat("/");
		for(UUID target : targets) {
			StringBuilder sb = new StringBuilder(httphost);
			sb.append(resourcemanager.add(target, this.resourcefile));
			sb.append(".zip");
			packsender.send(target, sb.toString(), this.sha1 == null ? resourcepacker.sha1 : this.sha1);
			positiontracker.setPlaylistInfo(target, this.id, sounds);
		}
	}
}
