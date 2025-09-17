package me.bomb.amusic.source;

import me.bomb.amusic.util.Crc;

public class OggVorbisSplitter implements Runnable {
	
	//TODO: Fix broken output sometimes
	
	private final byte[] srcOggVorbis;
	public byte[] part1, part2;
	
	public OggVorbisSplitter(byte[] srcOggVorbis) {
		this.srcOggVorbis = srcOggVorbis;
	}
	
	@Override
	public void run() {
		OggVorbisPageInfo vorbispageinfo;
		vorbispageinfo = new OggVorbisPageInfo(srcOggVorbis);
		final int[] pagestarts = vorbispageinfo.pagestarts, pagesizes = vorbispageinfo.pagesizes;
        final int pagecount = pagestarts.length;
        
        final int bof = pagestarts[pagecount-1];

        final int eof = pagestarts[0] + pagesizes[0];
        final int eof2 = srcOggVorbis.length;
        final int center = pagestarts[pagecount >>> 1];
        
        final int vorbisHeaderEnd = pagestarts[pagecount-3]; //Lets hope that vorbis header in first 2 ogg pages
        byte[] firstpart = new byte[center - bof];
        byte[] secondpart = new byte[eof - center + vorbisHeaderEnd];
        System.arraycopy(srcOggVorbis, bof, firstpart, 0, firstpart.length);
        System.arraycopy(srcOggVorbis, center, secondpart, vorbisHeaderEnd, secondpart.length - vorbisHeaderEnd);
        System.arraycopy(srcOggVorbis, bof, secondpart, 0, vorbisHeaderEnd); //Copy vorbis headers to second part
        
        int firoffset = pagestarts[(pagecount >>> 1) + 1];
        int firlength = pagesizes[(pagecount >>> 1) + 1];
        firstpart[firoffset + 5] |= 0x04; //EOS
        updateChecksum(firstpart, firoffset, firlength);
        
        //secondpart[5] |= 0x02; //BOS //Not needed since headers copied
        
        int secpage = pagecount >>> 1;
        ++secpage;
        int seq = 1;
        long startGranule = getGranule(secondpart, vorbisHeaderEnd + pagestarts[secpage-1] - center);
        while(--secpage > -1) {
        	int secoffset = vorbisHeaderEnd + pagestarts[secpage] - center;
        	int seclength = pagesizes[secpage];
        	setGranule(secondpart, secoffset, getGranule(secondpart, secoffset) - startGranule);
        	setSequence(secondpart, secoffset, ++seq);
        	updateChecksum(secondpart, secoffset, seclength);
        }
        
        part1 = firstpart; //OK;
        part2 = secondpart; //OK; may not work if vorbis header not in 2 first ogg pages
	}
	
	public long getGranule(final byte[] buf, final int offset) {
		int pagestartoffset = offset;
		pagestartoffset+=5;

		long granule = buf[++pagestartoffset] & 0xFF;
		granule |= (buf[++pagestartoffset] & 0xFF) << 8;
		granule |= (buf[++pagestartoffset] & 0xFF) << 16;
		granule |= (buf[++pagestartoffset] & 0xFF) << 24;
		granule |= (buf[++pagestartoffset] & 0xFF) << 32;
		granule |= (buf[++pagestartoffset] & 0xFF) << 40;
		granule |= (buf[++pagestartoffset] & 0xFF) << 48;
		granule |= (buf[++pagestartoffset] & 0xFF) << 56;
		return granule;
	}
	
	public void setGranule(final byte[] buf, final int offset, long granule) {
		int pagestartoffset = offset;
		pagestartoffset+=5;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
		granule >>>= 8;
		buf[++pagestartoffset] = (byte) granule;
	}
	
	public void setSequence(final byte[] buf, final int offset, int id) {
		int pagestartoffset = offset;
		pagestartoffset+=17;
		buf[++pagestartoffset] = (byte) id;
		id >>>= 8;
		buf[++pagestartoffset] = (byte) id;
		id >>>= 8;
		buf[++pagestartoffset] = (byte) id;
		id >>>= 8;
		buf[++pagestartoffset] = (byte) id;
	}
	
	public void updateChecksum(final byte[] buf, final int offset, int length) {
		int pagestartoffset = offset;
		pagestartoffset+=21;
		buf[++pagestartoffset] = 0;
		buf[++pagestartoffset] = 0;
		buf[++pagestartoffset] = 0;
		buf[++pagestartoffset] = 0;
		int crc = Crc.calculateCrc(0, buf, offset, length);
		pagestartoffset = offset;
		pagestartoffset+=21;
		buf[++pagestartoffset] = (byte) crc;
		crc >>>= 8;
		buf[++pagestartoffset] = (byte) crc;
		crc >>>= 8;
		buf[++pagestartoffset] = (byte) crc;
		crc >>>= 8;
		buf[++pagestartoffset] = (byte) crc;
	}
}
