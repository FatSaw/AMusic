package me.bomb.amusic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.bomb.amusic.Data.Options;

final class ResourcePacked extends Thread {
    private final String name;
    private final Set<UUID> targets = new HashSet<UUID>();
    private final Data data;
    private final File musicdir;
    private final File temppath;
    private final File amusicdir;
    private final List<String> songnames;
    private final List<Integer> songlengths;
    private final File resourcefile;
    private static final HashMap<UUID,PackInfo> downloadedplaylistinfo = new HashMap<UUID,PackInfo>();
    private static final Set<ResourcePacked> runningpackers = new HashSet<ResourcePacked>();
    private String sha1 = null;
    private boolean ok = false;
    
    private ResourcePacked(UUID target,Data data,String name, boolean update) throws NoSuchElementException {
    	this.name = name;
    	this.targets.add(target);
    	this.data = data;
    	temppath = new File(ConfigOptions.temppath + File.separator + name);
    	amusicdir = new File(temppath + File.separator + "assets" + File.separator + "minecraft" + File.separator + "sounds" + File.separator + "amusic");
    	
    	
        File musicpath = new File(ConfigOptions.musicpath.toString(),name);
        if(musicpath==null||!musicpath.isDirectory()) {
        	throw new NoSuchElementException();
        }
        musicdir = musicpath;
        List<String> asongnames = null;
        List<Integer> asonglengths = null;
        File aresourcefile = null;
        if(data.containsPlaylist(name)) {
        	Options options = data.getPlaylist(name);
        	File resourcefile = new File(ConfigOptions.packedpath.toString(), options.name);
        	if(resourcefile!=null&&resourcefile.exists()) {
            	if(update) {
                	delete(resourcefile);
                	data.removePlaylist(name);
            	} else if(options.size==resourcefile.length()&&options.sha1.equals(calcSHA1(resourcefile))) {
            		aresourcefile = resourcefile;
            		asongnames = options.sounds;
            		asonglengths = options.length;
            		this.sha1 = options.sha1;
            		ok = true;
            	}
        	}
        }
        if(!ok) {
        	asongnames = new ArrayList<String>();
        	asonglengths = new ArrayList<Integer>();
        	short zip;
            for (zip = 0;zip!=Short.MIN_VALUE&&new File(ConfigOptions.packedpath.toString(), "music" + zip + ".zip").exists(); ++zip) {}
            aresourcefile = new File(ConfigOptions.packedpath.toString(), "music" + zip + ".zip");
        }
        this.resourcefile = aresourcefile;
        songnames = asongnames;
        songlengths = asonglengths;
    }
    protected static List<String> getActivePlaylist(UUID playeruuid) {
    	if(downloadedplaylistinfo.containsKey(playeruuid)) {
        	return downloadedplaylistinfo.get(playeruuid).songs;
    	}
    	return null;
    }
    protected static List<Integer> getActiveLengths(UUID playeruuid) {
    	if(downloadedplaylistinfo.containsKey(playeruuid)) {
        	return downloadedplaylistinfo.get(playeruuid).lengths;
    	}
    	return null;
    }
    public String calcSHA1(File file) {
    	try {
    		FileInputStream fileInputStream = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
            byte[] bytes = new byte[1024];
            while (digestInputStream.read(bytes) > 0);
    		digestInputStream.close();
            byte[] resultByteArry = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : resultByteArry) {
                int value = b & 0xFF;
                if (value < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(value).toUpperCase());
            }
            return sb.toString();
    	} catch (IOException | NoSuchAlgorithmException e) {
    	}
    	return null;
    }
    private void delete(File file) {
        try {
            if(file.isDirectory()) {
                if (file.list().length == 0) {
                    file.delete();
                } else {
                    final String[] files = file.list();
                    String[] array;
                    for (int length = (array = files).length, i = 0; i < length; ++i) {
                        String temp = array[i];
                        File fileDelete = new File(file, temp);
                        this.delete(fileDelete);
                    }
                    if (file.list().length == 0) {
                        file.delete();
                    }
                }
            } else {
                file.delete();
            }
        } catch (Exception e) {
        }
    }
    protected static void remove(UUID playeruuid) {
    	ResourcePacked.downloadedplaylistinfo.remove(playeruuid);
    }
    protected static boolean load(Player player,Data data,String name,boolean update) {
    	PositionTracker.remove(player.getUniqueId());
    	ResourcePacked resourcepacked = null;
    	for(ResourcePacked packed : runningpackers) {
    		if(packed.name.equals(name)) {
    			resourcepacked = packed;
    			break;
    		}
    	}
    	if(resourcepacked!=null) {
    		synchronized (resourcepacked.targets) {
    			if(!resourcepacked.ok) {
            		resourcepacked.targets.add(player.getUniqueId());
            		downloadedplaylistinfo.put(player.getUniqueId(), resourcepacked.new PackInfo(resourcepacked.songnames,resourcepacked.songlengths));
            		return true;
    			}
			}
    	}
    	resourcepacked = new ResourcePacked(player.getUniqueId(), data, name, update);
    	if (resourcepacked.ok) {
            resourcepacked.send(player);
            downloadedplaylistinfo.put(player.getUniqueId(), resourcepacked.new PackInfo(resourcepacked.songnames,resourcepacked.songlengths));
        	return true;
        } else if (!resourcepacked.isAlive()) {
        	resourcepacked.start();
        	return true;
        }
    	return false;
    }
    private static int calculateDuration(File oggFile) {
    	try {
    		int rate = -1;
            int length = -1;

            int size = (int) oggFile.length();
            byte[] t = new byte[size];

            FileInputStream stream = new FileInputStream(oggFile);
            stream.read(t);

            for (int i = size-1-8-2-4; i>=0 && length<0; i--) { //4 bytes for "OggS", 2 unused bytes, 8 bytes for length
                // Looking for length (value after last "OggS")
                if (
                        t[i]==(byte)'O'
                        && t[i+1]==(byte)'g'
                        && t[i+2]==(byte)'g'
                        && t[i+3]==(byte)'S'
                ) {
                    byte[] byteArray = new byte[]{t[i+6],t[i+7],t[i+8],t[i+9],t[i+10],t[i+11],t[i+12],t[i+13]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    length = bb.getInt(0);
                }
            }
            for (int i = 0; i<size-8-2-4 && rate<0; i++) {
                // Looking for rate (first value after "vorbis")
                if (
                        t[i]==(byte)'v'
                        && t[i+1]==(byte)'o'
                        && t[i+2]==(byte)'r'
                        && t[i+3]==(byte)'b'
                        && t[i+4]==(byte)'i'
                        && t[i+5]==(byte)'s'
                ) {
                    byte[] byteArray = new byte[]{t[i+11],t[i+12],t[i+13],t[i+14]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    rate = bb.getInt(0);
                }

            }
            stream.close();
            return length/rate;
    	} catch (IOException e) {
    	}
        return 0;
    }
    protected void send(Player player) {
    	try {
        	StringBuilder sb = new StringBuilder("http://");
        	sb.append(ConfigOptions.host);
        	sb.append(":");
        	sb.append(ConfigOptions.port);
        	sb.append("/");
        	sb.append(CachedResource.add(player.getUniqueId(),this.resourcefile));
        	sb.append(".zip");
        	player.setResourcePack(sb.toString(),this.sha1);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    @Override
    public void run() {
    	runningpackers.add(this);
    	try {
    		if(!this.ok) {
        		delete(temppath);
        		List<File> musicfiles = new ArrayList<File>();
        		List<File> outmusicfiles = new ArrayList<File>();
        		for(File musicfile : musicdir.listFiles()) {
                	if(ConfigOptions.useconverter || musicfile.getName().endsWith(".ogg")) {
                		musicfiles.add(musicfile);
                    	String songname = musicfile.getName();
                    	if(songname.contains(".")) {
                    		songname = songname.substring(0, songname.lastIndexOf("."));
                    	}
                    	songnames.add(songname);
            		}
                }
        		amusicdir.mkdirs();
                Set<String> entrys = preparePack((byte)musicfiles.size());
                boolean asyncconvertation = musicfiles.size()>1&&ConfigOptions.encodetracksasynchronly;
                List<AtomicBoolean> convertationstatus = asyncconvertation?new ArrayList<AtomicBoolean>(musicfiles.size()):null;
                for(byte i = 0 ;musicfiles.size()>i;++i) {
                	File musicfile = musicfiles.get(i);
                	File outfile = new File(amusicdir, "music".concat(Byte.toString(i)).concat(".ogg"));
                	outmusicfiles.add(outfile);
            		if(ConfigOptions.useconverter) {
            			if(asyncconvertation) {
            				convertationstatus.add(Converter.convert(musicfile, outfile, asyncconvertation));
            			} else {
            				Converter.convert(musicfile, outfile, asyncconvertation);
            			}
            		} else {
            			try {
            				byte[] resource = new byte[ConfigOptions.maxmusicfilesize];
            				FileInputStream in = new FileInputStream(musicfile);
            				resource = Arrays.copyOf(resource, in.read(resource));
            				in.close();
            				FileOutputStream out = new FileOutputStream(outfile);
            				out.write(resource);
            				out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
            		}
                }
                if(asyncconvertation) {
                    boolean convertationrunning = true;
                    byte checkcount = 0;
                    while(convertationrunning) {
                    	if(checkcount==-1) {
                    		runningpackers.remove(this);
                    		return; //drop task if not finished for 4 minutes
                    	}
                    	try {
        					sleep(1000);
        				} catch (InterruptedException e) {
        				}
                    	boolean finished = true;
                    	for(AtomicBoolean status : convertationstatus) {
                    		finished&=status.get();
                    	}
                    	convertationrunning = !finished;
                    	++checkcount;
                    }
                }
                //calculatelengths
                for(File outfile : outmusicfiles) {
                	songlengths.add((int)Math.round(calculateDuration(outfile)));
                }
              //packing to archive
                CachedResource.resetCache(resourcefile.toPath());
        		try {
        			ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(resourcefile.toPath()));
        			zipOutputStream.setMethod(8);
        	        zipOutputStream.setLevel(5);
        	        for (String toadd : entrys) {
        	        	ZipEntry zipEntry = new ZipEntry(toadd);
        	            try {
        	                zipOutputStream.putNextEntry(zipEntry);
        	                Files.copy(new File(temppath, toadd).toPath(), zipOutputStream);
        	                zipOutputStream.closeEntry();
        	            } catch (IOException e) {
        	            }
        	        }
        	        zipOutputStream.close();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        		this.sha1 = calcSHA1(resourcefile);
        		
        		delete(temppath);
                data.setPlaylist(name, (int)resourcefile.length(), resourcefile.getName(), songnames, songlengths, this.sha1);
                data.save();
                data.load();
        	}
            this.ok = true;
    	} finally {
    		runningpackers.remove(this);
    	}
    	synchronized (targets) {
            for(UUID target : targets) {
                Player player = Bukkit.getPlayer(target);
                if (player != null) {
                	send(player);
                	downloadedplaylistinfo.put(player.getUniqueId(), new PackInfo(songnames,songlengths));
                }
            }
		}
    }
    private Set<String> preparePack(byte soundcount) {
    	File packfile = new File(temppath, "pack.mcmeta");
    	File soundsfile = new File(temppath, "assets" + File.separator + "minecraft" + File.separator + "sounds.json");
    	
    	try {
    		Set<String> entrys = new HashSet<String>(soundcount+2);
    		PrintWriter pack_out = new PrintWriter(new FileWriter(packfile));
            pack_out.write("{\n");
            pack_out.write("  \"pack\": {\n");
            pack_out.write("    \"pack_format\": 3,\n");
            pack_out.write("    \"description\": \"§4§lＡＭｕｓｉｃ\"\n");
            pack_out.write("  }\n");
            pack_out.write("}");
            pack_out.close();
            entrys.add("pack.mcmeta");
            
            PrintWriter sound_out = new PrintWriter(new FileWriter(soundsfile));
            sound_out.write("{");
            sound_out.println();
            byte i = 0;
            while(i<soundcount) {
            	sound_out.write("\t\"amusic.music" + i + "\": {\n");
                sound_out.write("\t\t\"category\": \"master\",\n");
                sound_out.write("\t\t\"sounds\": [\n");
                sound_out.write("\t\t\t{\n");
                sound_out.write("\t\t\t\t\"name\":\"amusic/music" + i + "\",\n");
                sound_out.write("\t\t\t\t\"stream\": true\n");
                sound_out.write("\t\t\t}\n");
                sound_out.write("\t\t]\n");
                if(soundcount-1==i) {
                	sound_out.write("\t}\n");
                } else {
                	sound_out.write("\t},\n");
                }
                entrys.add("assets" + "/" + "minecraft" + "/" + "sounds" + "/" + "amusic" + "/" + "music" + i + ".ogg");
                ++i;
            }
            sound_out.write("}");
            sound_out.flush();
            sound_out.close();
            entrys.add("assets" + "/" + "minecraft" + "/" + "sounds.json");
            return entrys;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    	return null;
    }
    private class PackInfo {
    	private final List<String> songs;
    	private final List<Integer> lengths;
    	private PackInfo(List<String> songs,List<Integer> lengths) {
    		this.songs = songs;
    		this.lengths = lengths;
    	}
    }
}
