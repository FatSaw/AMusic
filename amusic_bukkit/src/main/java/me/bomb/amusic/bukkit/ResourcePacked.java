package me.bomb.amusic.bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.Options;
import me.bomb.amusic.PackSender;
import me.bomb.amusic.PositionTracker;
import me.bomb.amusic.ResourcePacker;
import me.bomb.amusic.SoundInfo;
import me.bomb.amusic.bukkit.moveapplylistener.PackApplyListener;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourcePacked implements Runnable {
	private final String name;
	private final UUID target;
	private final Data data;
	private final ResourceManager resourcemanager;
	private final ResourcePacker resourcepacker;
	private final PositionTracker positiontracker;
	private final PackSender packsender;
	private final List<String> soundnames;
	private final List<Short> soundlengths;
	private final byte[] sha1;
	private final File resourcefile;

	private ResourcePacked(Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String name, boolean update) throws NoSuchElementException {
		this.name = name;
		this.target = target;
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.packsender = packsender;
		File musicdir = new File(ConfigOptions.musicpath.toString(), name);
		File tempdir = new File(ConfigOptions.temppath.toString(), name);
		File sourcearchive = new File(ConfigOptions.musicpath.toString(), name.concat(".zip"));
		List<String> asongnames = null;
		List<Short> asonglengths = null;
		byte[] asha1 = null;
		File resourcefile = null;
		boolean ok = false;
		if (data.containsPlaylist(name)) {
			Options options = data.getPlaylist(name);
			resourcefile = new File(ConfigOptions.packedpath.toString(), options.name);
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
				for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(ConfigOptions.packedpath.toString(), "music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
				}
				resourcefile = aresourcefile;
			}
			this.resourcepacker = new ResourcePacker(ConfigOptions.useconverter, ConfigOptions.bitrate, ConfigOptions.channels, ConfigOptions.samplingrate, ConfigOptions.encodetracksasynchronly, ConfigOptions.maxpacksize, ConfigOptions.maxmusicfilesize, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
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

	private ResourcePacked(Data data, ResourceManager resourcemanager, PositionTracker positiontracker, String name) throws NoSuchElementException {
		this.data = data;
		this.resourcemanager = resourcemanager;
		this.positiontracker = positiontracker;
		this.name = name;
		this.target = null;
		this.packsender = null;
		File musicdir = new File(ConfigOptions.musicpath.toString(), name);
		File tempdir = new File(ConfigOptions.temppath.toString(), name);
		File sourcearchive = new File(ConfigOptions.musicpath.toString(), name.concat(".zip"));
		if (!data.containsPlaylist(name)) {
			throw new NoSuchElementException();
		}
		Options options = data.getPlaylist(name);
		File resourcefile = new File(ConfigOptions.packedpath.toString(), options.name);
		if (resourcefile != null && resourcefile.exists()) {
			delete(resourcefile);
			data.removePlaylist(name);
		}
		File aresourcefile = null;

		if (!musicdir.exists()) {
			throw new NoSuchElementException();
		}
		for (short zip = 0; zip != Short.MIN_VALUE && (aresourcefile = new File(ConfigOptions.packedpath.toString(),
				"music".concat(Short.toString(zip)).concat(".zip"))).exists(); ++zip) {
		}
		this.resourcefile = aresourcefile;
		this.resourcepacker = new ResourcePacker(ConfigOptions.useconverter, ConfigOptions.bitrate, ConfigOptions.channels, ConfigOptions.samplingrate, ConfigOptions.encodetracksasynchronly, ConfigOptions.maxpacksize, ConfigOptions.maxmusicfilesize, musicdir, tempdir, resourcefile, sourcearchive.isFile() ? sourcearchive : null, resourcemanager, this);
		soundnames = resourcepacker.soundnames;
		soundlengths = resourcepacker.soundlengths;
		this.sha1 = resourcepacker.sha1;
	}

	public static boolean load(Data data, ResourceManager resourcemanager, PositionTracker positiontracker, PackSender packsender, UUID target, String name, boolean update) {
		boolean processpack = ConfigOptions.processpack;
		if (target == null && processpack) {
			ResourcePacked resourcepacked = new ResourcePacked(data, resourcemanager, positiontracker, name);

			if (resourcepacked.resourcepacker != null && !resourcepacked.resourcepacker.isAlive()) {
				resourcepacked.resourcepacker.start();
				return true;
			}
			return false;
		}
		update &= processpack;
		ResourcePacked resourcepacked = new ResourcePacked(data, resourcemanager, positiontracker, packsender, target, name, update);
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
		Player player = Bukkit.getPlayer(target);
		int soundssize = soundnames.size();
		if (player == null || soundssize != soundlengths.size()) {
			return;
		}
		StringBuilder sb = new StringBuilder("http://");
		sb.append(ConfigOptions.host);
		sb.append(":");
		sb.append(ConfigOptions.port);
		sb.append("/");
		sb.append(resourcemanager.add(target, this.resourcefile));
		sb.append(".zip");
		packsender.send(target, sb.toString(), this.sha1 == null ? resourcepacker.sha1 : this.sha1);
		ArrayList<SoundInfo> soundinfos = new ArrayList<SoundInfo>(soundssize);
		for(int i=0;i<soundssize;++i) {
			soundinfos.add(new SoundInfo(soundnames.get(i), soundlengths.get(i)));
		}
		if(!ConfigOptions.checkpackstatus) {
			resourcemanager.setAccepted(target);
			PackApplyListener.reset(target);
		}
		
		if(!ConfigOptions.packapplystatus) {
			positiontracker.setPlaylistInfo(target, name, soundinfos);
			return;
		}
		
		byte i = 0;
		while(++i!=0) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
			if(!player.isOnline()) {
				return;
			}
			if(PackApplyListener.applied(target)) {
				positiontracker.setPlaylistInfo(target, name, soundinfos);
				return;
			}
		}
	}
}
