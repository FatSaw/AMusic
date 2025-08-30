package me.bomb.amusic.source;

public class OggVorbisSplitter implements Runnable {
	
	//TODO: Fix invalid size
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
        int pagecount = pagestarts.length;
        
        final int bof = pagestarts[pagecount-1];

        final int eof = pagestarts[0] + pagesizes[0];
        final int eof2 = srcOggVorbis.length;
        final int center = pagestarts[pagecount >>> 1];

        System.out.println("pagecount: " + pagecount);
        System.out.println("BOF: " + bof);
        System.out.println("EOF: " + eof);

        System.out.println("EOF2: " + eof2);
        System.out.println("center: " + center);
        
        System.out.println(vorbispageinfo.toString());
        
        final int vorbisHeaderEnd = pagestarts[pagecount-3]; //Lets hope that vorbis header in first 2 ogg pages
        byte[] firstpart = new byte[center - bof];
        byte[] secondpart = new byte[eof - center + vorbisHeaderEnd];
        System.arraycopy(srcOggVorbis, bof, firstpart, 0, firstpart.length);
        System.arraycopy(srcOggVorbis, center, secondpart, vorbisHeaderEnd, secondpart.length - vorbisHeaderEnd);
        System.arraycopy(srcOggVorbis, bof, secondpart, 0, vorbisHeaderEnd); //Copy vorbis headers to second part

		int offset = 0;
        offset = srcOggVorbis.length - pagestarts[0];
        
        offset = pagestarts[(pagecount >>> 1) + 1];
        
        byte i = 14;
        while(--i > 5) {
        	firstpart[offset + i] = 0;
        }
        firstpart[offset + 5] |= 0x04; //EOS
        
        offset = pagestarts[0] - center;
        i = 14;
        while(--i > 5) {
        	secondpart[offset + i] = 0;
        }
        
        //secondpart[5] |= 0x02; //BOS //Not needed since headers copied
        int secondPos = pagecount >>> 1;
        offset = vorbisHeaderEnd + 6;
        long totalPos = 0;
        while(--secondPos > 0) {
        	int granulePos = offset + pagestarts[secondPos] - center;
        	long granulePosition = secondpart[granulePos + 7] & 0xFF << 56;
        	granulePosition |= secondpart[granulePos + 6] & 0xFF << 48;
        	granulePosition |= secondpart[granulePos + 5] & 0xFF << 40;
        	granulePosition |= secondpart[granulePos + 4] & 0xFF << 32;
        	granulePosition |= secondpart[granulePos + 3] & 0xFF << 24;
        	granulePosition |= secondpart[granulePos + 2] & 0xFF << 16;
        	granulePosition |= secondpart[granulePos + 1] & 0xFF << 8;
        	granulePosition |= secondpart[granulePos + 0] & 0xFF;
        	totalPos += granulePosition;
        	
        }

    	int granulePos = offset + pagestarts[secondPos] - center;
        secondpart[granulePos + 0] = (byte) totalPos;
        totalPos>>>=8;
    	secondpart[granulePos + 1] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 2] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 3] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 4] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 5] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 6] = (byte) totalPos;
    	totalPos>>>=8;
    	secondpart[granulePos + 7] = (byte) totalPos;
        
        part1 = firstpart; //OK;
        part2 = secondpart; //OK; may not work if vorbis header not in 2 first ogg pages
	}
}
