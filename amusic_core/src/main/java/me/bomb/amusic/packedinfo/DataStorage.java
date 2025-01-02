package me.bomb.amusic.packedinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import static me.bomb.amusic.util.Base64Utils.toBase64Url;
import static me.bomb.amusic.util.Base64Utils.fromBase64Url;

public class DataStorage extends me.bomb.amusic.packedinfo.Data {
	private static final String FORMAT = ".ampi";
	private static final FilenameFilter AMPIFILTER = new FilenameFilter() {
		@Override
		public boolean accept(File parent, String name) {
			return name.endsWith(FORMAT);
		}
	};
	private static final byte VERSION = 2;
	public final File datadirectory;
	private final DataSaveThread[] savethreads;
	
	public DataStorage(File datadirectory, boolean lockwrite, byte savethreadcount) {
		super(lockwrite);
		this.datadirectory = datadirectory;
		if(savethreadcount < 0) {
			savethreadcount = 0;
		}
		this.savethreads = new DataSaveThread[savethreadcount];
		while(--savethreadcount > -1) {
			this.savethreads[savethreadcount] = new DataSaveThread(datadirectory);
		}
	}

	@Override
	protected void save() {
		for(File file : datadirectory.listFiles(AMPIFILTER)) {
			String name = file.getName();
			name = name.substring(0, name.length() - FORMAT.length());
			name = fromBase64Url(name);
			if(!options.containsKey(name)) {
				file.delete();
			}
		}
		Entry<String, DataEntry>[] arr = new Entry[options.size()];
		options.entrySet().toArray(arr);
		int entrycount = arr.length, threadcount = savethreads.length;
		int threadsavecount = entrycount / threadcount;
		++threadsavecount;
		byte curthread = 0;
		int previousnums = 0;
		while(curthread < threadcount) {
			DataSaveThread thread = this.savethreads[curthread];
			int nums = threadsavecount;
			nums+=previousnums;
			if(nums > entrycount) {
				nums -= nums - entrycount;
			}
			if(previousnums==nums) {
				break;
			}
			thread.save(arr, previousnums, nums);
			previousnums=nums;
			++curthread;
		}
		
	}

	@Override
	public void load() {
		options.clear();
		File[] files = datadirectory.listFiles(AMPIFILTER);
		for(File file : files) {
			String id = file.getName();
			id = id.substring(0, id.length() - FORMAT.length());
			id = fromBase64Url(id);
			try {
				FileInputStream fis = new FileInputStream(file);
				byte[] buf = new byte[8];
				if(fis.read(buf) != 8 || buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'p' || buf[3] != 'i' || buf[4] != 0 || buf[7] != 0) {
					fis.close();
					continue;
				}
				byte version = (byte) fis.read();
				buf = new byte[4];
				byte[] sha1 = new byte[20];
				int packednamelength;
				fis.read(buf);
				fis.read(sha1);
				if(version != VERSION || (packednamelength = fis.read()) == -1) {
					fis.close();
					continue;
				}
				int packedsize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
				buf = new byte[packednamelength];
				fis.read(buf);
				String packedname = new String(buf, StandardCharsets.UTF_8);
				buf = new byte[2];
				if(fis.read(buf) != 2) {
					fis.close();
					continue;
				}
				int soundcount = 0x0000FFFF;
				soundcount &= 0xFF & buf[0] | buf[1] << 8;
				byte[] namelengths = new byte[soundcount];
				buf = new byte[soundcount<<1];
				fis.read(namelengths);
				short[] lengths = new short[soundcount];
				fis.read(buf);
				for(int k = 0,j = 0; k < soundcount; ++k, ++j) {
					lengths[k] = (short) (buf[j] & 0xFF | buf[++j]<<8);
				}
				soundcount = (short) lengths.length;
				SoundInfo[] sounds = new SoundInfo[soundcount];
				int i = 0;
				while(i < soundcount) {
					buf = new byte[0xFF & namelengths[i]];
					fis.read(buf);
					sounds[i] = new SoundInfo(new String(buf, StandardCharsets.UTF_8), lengths[i]);
					++i;
				}
				fis.close();
				DataEntry dataentry = new DataEntry(packedsize, packedname, sounds, sha1);
				dataentry.saved = true;
				options.put(id, dataentry);
			} catch (IOException e) {
				continue;
			}
		}
	}
	
	public void start() {
		int threadcount = savethreads.length;
		while(--threadcount > -1) {
			savethreads[threadcount].start();
		}
	}
	
	public void end() {
		int threadcount = savethreads.length;
		while(--threadcount > -1) {
			savethreads[threadcount].end();
		}
	}
	
	protected final class DataSaveThread extends Thread {
		
		private final File datadirectory;
		private boolean run = true;
		private Entry<String, DataEntry>[] list;
		private int start, end;
		
		protected DataSaveThread(File datadirectory) {
			this.datadirectory = datadirectory;
		}
		
		protected boolean save(Entry<String, DataEntry>[] list, int start,int end) {
			if(!run) return false;
			this.list = list;
			this.start = start;
			this.end = end;
			synchronized(this) {
				this.notify();
			}
			return true;
		}
		
		@Override
		public void run() {
			while(run) {
				Entry<String, DataEntry>[] list;
				list = this.list;
				if(list==null || end < start) {
					synchronized(this) {
						try {
							wait();
						} catch (InterruptedException e) {
						}
					}
					continue;
				}
				
				while(start < end) {
					Entry<String, DataEntry> entry = list[start];
					String id = entry.getKey();
					id = toBase64Url(id);
					DataEntry dataentry = entry.getValue();
					File amusicpackedinfo = new File(datadirectory, id.concat(FORMAT));
					int soundcount = dataentry.sounds.length;
					if(dataentry.checkValues() || (dataentry.saved && amusicpackedinfo.exists()) || dataentry.sha1.length != 20 || dataentry.size < 0) {
						++start;
						continue;
					}
					if(soundcount > 0x0000FFFF) {
						soundcount = 0x0000FFFF;
					}
					
					try {
						FileOutputStream fos = new FileOutputStream(amusicpackedinfo, false);
						fos.write('a'); //FORMATID
						fos.write('m'); //FORMATID
						fos.write('p'); //FORMATID
						fos.write('i'); //FORMATID
						fos.write(0); //FORMATID
						fos.write(0); //0
						fos.write(0); //0
						fos.write(0); //FORMATID
						fos.write(VERSION); //VERSION
						int entryfilesize = dataentry.size;
						//fos.write(dataentry.size);
						fos.write((byte)entryfilesize); //FILESIZE
						entryfilesize>>=8;
						fos.write((byte)entryfilesize); //FILESIZE
						entryfilesize>>=8;
						fos.write((byte)entryfilesize); //FILESIZE
						entryfilesize>>=8;
						fos.write((byte)entryfilesize); //FILESIZE
						fos.write(dataentry.sha1); //SHA1
						byte[] packednamebytes = dataentry.name.getBytes(StandardCharsets.UTF_8);
						int packednamelength = packednamebytes.length;
						if(packednamelength > 0xFF) {
							packednamelength = 0xFF;
							byte[] npackednamebytes = new byte[0xFF];
							System.arraycopy(packednamebytes, 0, npackednamebytes, 0, packednamelength);
							packednamebytes = npackednamebytes;
						}
						fos.write((byte) packednamelength); //PACKED FILE PATH
						fos.write(packednamebytes); //PACKED FILE PATH
						byte[] soundcountb = new byte[2];
						soundcountb[0] = (byte) soundcount;
						soundcountb[1] = (byte) (soundcount>>8);
						fos.write(soundcountb);
						int lengthscount = soundcount<<1;
						byte[] namelengths = new byte[soundcount], lengths = new byte[lengthscount];
						short i = 0, j = 0;
						int totalsoundnamelength = 0;
						byte[][] anames = new byte[soundcount][];
						while(i < soundcount) {
							byte[] soundnamebytes = dataentry.sounds[i].name.getBytes(StandardCharsets.UTF_8);
							int soundnamelength = soundnamebytes.length;
							if(soundnamelength > 0xFF) {
								soundnamelength = 0xFF;
								byte[] nsoundnamebytes = new byte[0xFF];
								System.arraycopy(soundnamebytes, 0, nsoundnamebytes, 0, soundnamelength);
								soundnamebytes = nsoundnamebytes;
							}
							totalsoundnamelength += soundnamelength;
							anames[i] = soundnamebytes;
							namelengths[i] = (byte) soundnamelength;
							short length = dataentry.sounds[i].length;
							++i;
							lengths[j] = (byte) length;
							length >>= 8;
							++j;
							lengths[j] = (byte) length;
							++j;
						}
						byte[] names = new byte[totalsoundnamelength];
						int namesi = 0;
						i = 0;
						while(i < soundcount) {
							byte[] soundnamebytes = anames[i];
							int soundnamelength = soundnamebytes.length;
							System.arraycopy(soundnamebytes, 0, names, namesi, soundnamelength);
							namesi+=soundnamelength;
							++i;
						}
						fos.write(namelengths); //NAME LENGTHS  ENTRY 0-255
						fos.write(lengths); //SOUND LENGTHS  ENTRY 0-65535
						fos.write(names); //SOUND LENGTHS ALL 0-8355585 32767*255
						
						fos.close();
						dataentry.saved = true;
					} catch (IOException e) {
					}
					++start;
				}
				list = null;
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		protected void end() {
			run = false;
			this.list = null;
			synchronized(this) {
				this.notify();
			}
		}
		
	}

}
