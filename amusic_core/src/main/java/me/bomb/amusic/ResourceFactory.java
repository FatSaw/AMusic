package me.bomb.amusic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourceFactory implements Runnable {
	private final String name;
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

	private ResourceFactory(ConfigOptions configoptions, Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String name, boolean update) throws NoSuchElementException {
		this.name = name;
		this.target = target;
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		File musicdir = new File(configoptions.musicdir, name);
		File tempdir = new File(configoptions.tempdir, name);
		File sourcearchive = new File(configoptions.musicdir, name.concat(".zip"));
		List<String> asongnames = null;
		List<Short> asonglengths = null;
		byte[] asha1 = null;
		File resourcefile = null;
		boolean ok = false;
		if (data.containsPlaylist(name)) {
			DataEntry options = data.getPlaylist(name);
			resourcefile = new File(configoptions.packeddir, options.name);
			if (resourcefile != null && resourcefile.exists()) {
				if (update) {
					delete(resourcefile);
					data.removePlaylist(name);
				} else if (resourcemanager.isCached(resourcefile.toPath()) || options.check(resourcefile)) {
					asongnames = options.sounds;
					asonglengths = options.length;
					asha1 = options.sha1;
					ok = true;
				}
			}
		}
		if (!ok) {
			if (!musicdir.exists()) {
				throw new NoSuchElementException();
			}
			if(resourcefile==null) {
				File aresourcefile = null;
				for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(configoptions.packeddir, "music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
				}
				resourcefile = aresourcefile;
			}
			this.resourcepacker = new ResourcePacker(configoptions.useconverter, configoptions.bitrate, configoptions.channels, configoptions.samplingrate, configoptions.encodetracksasynchronly, configoptions.maxpacksize, configoptions.maxmusicfilesize, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
			this.soundnames = resourcepacker.soundnames;
			this.soundlengths = resourcepacker.soundlengths;
			this.sha1 = null;
			this.resourcefile = resourcefile;
		} else {
			this.resourcepacker = null;
			this.soundnames = asongnames;
			this.soundlengths = asonglengths;
			this.sha1 = asha1;
			this.resourcefile = resourcefile;
			new Thread(this).start();
		}
	}

	private ResourceFactory(ConfigOptions configoptions,Data data, ResourceManager resourcemanager, PositionTracker positiontracker, String name) throws NoSuchElementException {
		this.configoptions = configoptions;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.name = name;
		this.target = null;
		this.packsender = null;
		File musicdir = new File(configoptions.musicdir, name);
		File tempdir = new File(configoptions.tempdir, name);
		File sourcearchive = new File(configoptions.musicdir, name.concat(".zip"));
		if (!data.containsPlaylist(name)) {
			throw new NoSuchElementException();
		}
		DataEntry options = data.getPlaylist(name);
		File resourcefile = new File(configoptions.packeddir, options.name);
		if (resourcefile != null && resourcefile.exists()) {
			delete(resourcefile);
			data.removePlaylist(name);
		}
		File aresourcefile = null;

		if (!musicdir.exists()) {
			throw new NoSuchElementException();
		}
		for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(configoptions.packeddir,
				"music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
		}
		this.resourcefile = aresourcefile;
		this.resourcepacker = new ResourcePacker(configoptions.useconverter, configoptions.bitrate, configoptions.channels, configoptions.samplingrate, configoptions.encodetracksasynchronly, configoptions.maxpacksize, configoptions.maxmusicfilesize, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
		soundnames = resourcepacker.soundnames;
		soundlengths = resourcepacker.soundlengths;
		this.sha1 = resourcepacker.sha1;
	}

	public static boolean load(ConfigOptions configoptions,Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String name, boolean update) {
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
	
	private static void delete(File file) {
		try {
			if (file.isDirectory()) {
				if (file.list().length == 0) {
					file.delete();
					return;
				}
				final String[] files = file.list();
				String[] array;
				for (int length = (array = files).length, i = 0; i < length; ++i) {
					String temp = array[i];
					File fileDelete = new File(file, temp);
					delete(fileDelete);
				}
				if (file.list().length == 0) {
					file.delete();
				}
			} else {
				file.delete();
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void run() {
		if(this.resourcepacker != null) {
			data.setPlaylist(name, soundnames, soundlengths, resourcefile);
			data.save();
			data.load();
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
		sb.append(":");
		sb.append(configoptions.port);
		sb.append("/");
		sb.append(resourcemanager.add(target, this.resourcefile));
		sb.append(".zip");
		packsender.send(target, sb.toString(), this.sha1 == null ? resourcepacker.sha1 : this.sha1);
		ArrayList<SoundInfo> soundinfos = new ArrayList<SoundInfo>(soundssize);
		for(int i=0;i<soundssize;++i) {
			soundinfos.add(new SoundInfo(soundnames.get(i), soundlengths.get(i)));
		}
		positiontracker.setPlaylistInfo(target, name, soundinfos);
	}
}
