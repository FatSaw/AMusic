package me.bomb.amusic.source;

public class OggVorbisSplitter implements Runnable {
	
	private final byte[] srcOggVorbis;
	public byte[] part1, part2;
	
	public OggVorbisSplitter(byte[] srcOggVorbis) {
		this.srcOggVorbis = srcOggVorbis;
	}
	
	@Override
	public void run() {
		int offset = 0;
		int pagecount = 0;
        while (offset < srcOggVorbis.length) {
            if (srcOggVorbis[offset] == 'O' && srcOggVorbis[offset + 1] == 'g' && srcOggVorbis[offset + 2] == 'g' && srcOggVorbis[offset + 3] == 'S') {
                offset += 26;
                ++pagecount;
            } else {
                offset++;
            }
        }
        int[] pagestarts = new int[pagecount], pagesizes = new int[pagecount];
        offset = 0;
        while (offset < srcOggVorbis.length) {
            if (srcOggVorbis[offset] == 'O' && srcOggVorbis[offset + 1] == 'g' && srcOggVorbis[offset + 2] == 'g' && srcOggVorbis[offset + 3] == 'S') {
                --pagecount;
                pagestarts[pagecount] = offset;
                offset+=26;
                int segmentCount = srcOggVorbis[offset] & 0xFF;
                int size = 26;
                while(--segmentCount > -1) {
                	int segmentSize = srcOggVorbis[offset + segmentCount] & 0xFF;
                	size+=segmentSize;
                	offset += segmentSize;
                }
                pagesizes[pagecount] = size;
                
            } else {
                ++offset;
            }
        }
        pagecount = pagestarts.length;

        final int bof = pagestarts[pagecount-1];
        final int eof = pagesizes[0] + pagestarts[0];
        final int center = pagestarts[pagecount >>> 1];
        byte[] firstpart = new byte[center - bof];
        byte[] secondpart = new byte[eof - center];
        System.arraycopy(srcOggVorbis, bof, firstpart, 0, firstpart.length);
        System.arraycopy(srcOggVorbis, center, secondpart, 0, secondpart.length);
        
        firstpart[pagestarts[(pagecount >>> 1) + 1] + 5] |= 0x04; //EOS
        secondpart[5] |= 0x02; //BOS
        
        part1 = firstpart; //OK
        part2 = secondpart; //NOT PLAYS MAYBE MISSING VORBIS HEADER 
	}
}
