package me.bomb.amusic.packedinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ResourcepackInfo {
	
	private static final byte VERSION = 5;
	
	protected int infosize;
	
	public int packsize;
	public String packname;
	public SoundInfo[] sounds;
	public byte[] sha1, sha256;
	public UUID bhea, bres;
	
	protected ResourcepackInfo(int infosize, int packsize, String packname, SoundInfo[] sounds, byte[] sha1, byte[] sha256, UUID bhea, UUID bres) {
		this.infosize = infosize;
		this.packsize = packsize;
		this.packname = packname;
		this.sounds = sounds;
		this.sha1 = sha1;
		this.sha256 = sha256;
		this.bhea = bhea;
		this.bres = bres;
	}
	
	public static int serialize(OutputStream os, ResourcepackInfo info) throws IOException {
		if(os == null || info.packsize < 0 || info.packname == null || info.sounds == null || info.sha1 == null || info.sha1.length != 20|| info.sha256 == null || info.sha256.length != 0x20) {
			throw new IllegalArgumentException();
		}
		int infosize = 100;
		int soundcount = info.sounds.length;
		os.write('a'); //FORMATID
		os.write('m'); //FORMATID
		os.write('p'); //FORMATID
		os.write('i'); //FORMATID
		os.write(0); //FORMATID
		os.write(0); //0
		os.write(0); //0
		os.write(0); //FORMATID
		os.write(VERSION); //VERSION
		int entryfilesize = info.packsize;
		//fos.write(dataentry.size);
		os.write((byte)entryfilesize); //FILESIZE
		entryfilesize>>>=8;
		os.write((byte)entryfilesize); //FILESIZE
		entryfilesize>>>=8;
		os.write((byte)entryfilesize); //FILESIZE
		entryfilesize>>>=8;
		os.write((byte)entryfilesize); //FILESIZE
		os.write(info.sha1); //SHA1
		os.write(info.sha256); //SHA256
		byte[] buuids = new byte[0x20];
		{
			int k = buuids.length;
			long msb = info.bhea.getMostSignificantBits(), lsb = info.bhea.getLeastSignificantBits();
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			msb = info.bres.getMostSignificantBits();
			lsb = info.bres.getLeastSignificantBits();
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			msb>>>=8;
			buuids[--k] = (byte) msb;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
			lsb>>>=8;
			buuids[--k] = (byte) lsb;
		}
		os.write(buuids);
		byte[] packednamebytes = info.packname.getBytes(StandardCharsets.UTF_8);
		int packednamelength = packednamebytes.length;
		if(packednamelength > 0xFF) {
			packednamelength = 0xFF;
			byte[] npackednamebytes = new byte[0xFF];
			System.arraycopy(packednamebytes, 0, npackednamebytes, 0, packednamelength);
			packednamebytes = npackednamebytes;
		}
		os.write((byte) packednamelength); //PACKED FILE PATH
		infosize += packednamelength;
		os.write(packednamebytes); //PACKED FILE PATH
		if(soundcount > 0x0000FFFF) {
			soundcount = 0x0000FFFF;
		}
		byte[] soundcountb = new byte[2];
		soundcountb[0] = (byte) soundcount;
		soundcountb[1] = (byte) (soundcount>>>8);
		os.write(soundcountb);
		int lengthscount = soundcount<<1;
		infosize += soundcount<<4; //SOUND HASH
		infosize += soundcount<<2;
		byte[] soundhashs = new byte[soundcount<<4], namelengths = new byte[soundcount], splits = new byte[soundcount],lengths = new byte[lengthscount];
		int i = soundcount, j = soundcount<<1, k = soundcount<<4;
		int totalsoundnamelength = 0;
		byte[][] anames = new byte[soundcount][];
		while(--i > -1) {
			SoundInfo soundinfo = info.sounds[i];
			UUID hash = soundinfo.hash;
			long msb = hash.getMostSignificantBits(), lsb = hash.getLeastSignificantBits();
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			msb>>>=8;
			soundhashs[--k] = (byte) msb;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			lsb>>>=8;
			soundhashs[--k] = (byte) lsb;
			
			byte[] soundnamebytes = soundinfo.name.getBytes(StandardCharsets.UTF_8);
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
			splits[i] = soundinfo.split;
			short length = soundinfo.length;
			lengths[--j] = (byte) length;
			length >>>= 8;
			lengths[--j] = (byte) length;
		}
		infosize += totalsoundnamelength;
		byte[] names = new byte[totalsoundnamelength];
		int namesi = 0;
		i = soundcount;
		while(--i > -1) {
			byte[] soundnamebytes = anames[i];
			int soundnamelength = soundnamebytes.length;
			System.arraycopy(soundnamebytes, 0, names, namesi, soundnamelength);
			namesi+=soundnamelength;
		}
		os.write(soundhashs); //SOUND HASHS ENTRY
		os.write(namelengths); //NAME LENGTHS ENTRY 0-255
		os.write(splits); //SOUND SPLITS ENTRY 0-255
		os.write(lengths); //SOUND LENGTHS ENTRY 0-65535
		os.write(names); //SOUND LENGTHS ALL 0-8355585 32767*255
		info.infosize = infosize;
		return infosize;
	}
	
	public static ResourcepackInfo deserialize(InputStream is) throws IOException {
		if(is == null) {
			throw new IllegalArgumentException();
		}
		int infosize = 100;
		byte[] buf = new byte[8];
		if(is.read(buf) != 8 || buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'p' || buf[3] != 'i' || buf[4] != 0 || buf[7] != 0) {
			is.close();
			//INVALID HEADER
			return null;
		}
		byte version = (byte) is.read();
		buf = new byte[4];
		byte[] sha1 = new byte[20];
		byte[] sha256 = new byte[0x20];
		byte[] buuids = new byte[0x20];
		int packednamelength;
		is.read(buf);
		is.read(sha1);
		is.read(sha256);
		is.read(buuids);
		if(version != VERSION || (packednamelength = is.read()) == -1) {
			is.close();
			//INVALID VERSION
			return null;
		}
		infosize+=packednamelength;
		int packedsize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
		UUID bhea, bres;
		{
			int j = buuids.length;
			long msb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56, lsb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
			bhea = new UUID(msb, lsb);
			msb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
			lsb = (buuids[--j] & 0xFFL) | (buuids[--j] & 0xFFL) << 8 | (buuids[--j] & 0xFFL) << 16 | (buuids[--j] & 0xFFL) << 24 | (buuids[--j] & 0xFFL) << 32 | (buuids[--j] & 0xFFL) << 40 | (buuids[--j] & 0xFFL) << 48 | (buuids[--j] & 0xFFL) << 56;
			bres = new UUID(msb, lsb);
		}
		buf = new byte[packednamelength];
		is.read(buf);
		String packedname = new String(buf, StandardCharsets.UTF_8);
		buf = new byte[2];
		is.read(buf);
		int soundcount = 0x0000FFFF;
		soundcount &= 0xFF & buf[0] | buf[1] << 8;
		infosize+=soundcount<<4; //SOUND HASH
		infosize+=soundcount<<2;
		buf = new byte[soundcount<<4];
		is.read(buf);
		UUID[] soundhashs = new UUID[soundcount];
		int i = soundcount, j = soundcount << 4;
		//--j;
		while(--i > -1) {
			long msb = (buf[--j] & 0xFFL) | (buf[--j] & 0xFFL) << 8 | (buf[--j] & 0xFFL) << 16 | (buf[--j] & 0xFFL) << 24 | (buf[--j] & 0xFFL) << 32 | (buf[--j] & 0xFFL) << 40 | (buf[--j] & 0xFFL) << 48 | (buf[--j] & 0xFFL) << 56, lsb = (buf[--j] & 0xFFL) | (buf[--j] & 0xFFL) << 8 | (buf[--j] & 0xFFL) << 16 | (buf[--j] & 0xFFL) << 24 | (buf[--j] & 0xFFL) << 32 | (buf[--j] & 0xFFL) << 40 | (buf[--j] & 0xFFL) << 48 | (buf[--j] & 0xFFL) << 56;
			soundhashs[i] = new UUID(msb, lsb);
		}
		byte[] namelengths = new byte[soundcount], splits = new byte[soundcount];
		buf = new byte[soundcount<<1];
		is.read(namelengths);
		is.read(splits);
		short[] lengths = new short[soundcount];
		is.read(buf);
		i = soundcount;
		j = soundcount<<1;
		while(--i > -1) {
			lengths[i] = (short) (buf[--j] & 0xFF | buf[--j]<<8);
		}
		soundcount = (short) lengths.length;
		SoundInfo[] sounds = new SoundInfo[soundcount];
		i = soundcount;
		while(--i > -1) {
			buf = new byte[0xFF & namelengths[i]];
			is.read(buf);
			infosize+=buf.length;
			sounds[i] = new SoundInfo(new String(buf, StandardCharsets.UTF_8), soundhashs[i], lengths[i], splits[i]);
		}
		return new ResourcepackInfo(infosize, packedsize, packedname, sounds, sha1, sha256, bhea, bres);
	}

}
