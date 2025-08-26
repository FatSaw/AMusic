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
		int offset = 0;
		int pagecount = 0;
        while (offset < srcOggVorbis.length) {
            if (srcOggVorbis[offset] == 'O' && srcOggVorbis[offset + 1] == 'g' && srcOggVorbis[offset + 2] == 'g' && srcOggVorbis[offset + 3] == 'S') {
                offset += 26;
                if(offset < srcOggVorbis.length) {
                    ++pagecount;
                }
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
                
                try {
                    int segmentCount = srcOggVorbis[offset] & 0xFF;
                    ++offset;
                    int size = 26;
                    while(--segmentCount > -1) {
                    	int segmentSize = srcOggVorbis[offset + segmentCount] & 0xFF;
                    	size += segmentSize;
                    	offset += segmentSize;
                    }
                    pagesizes[pagecount] = size;
                } catch (ArrayIndexOutOfBoundsException e) {
                    pagesizes[pagecount] = srcOggVorbis.length - pagestarts[pagecount];
                }
            } else {
                ++offset;
            }
        }
        pagecount = pagestarts.length;

        final int bof = pagestarts[pagecount-1];
        final int eof = pagesizes[0] + pagestarts[0];
        final int center = pagestarts[pagecount >>> 1];
        
        /*offset = bof + 26;

    	int[] vorbisstart = new int[3], vorbissize = new int[3];
        {
        	//SEARCH VORBIS HEADERS
        	int pageid = pagecount;
        	while(--pageid > -1) {
        		int start = pagestarts[pageid];
        		int size = pagesizes[pageid];
        		int end = start + size;
        		end -= 6;
        		offset = start;
        		while (offset < end) {
        			if (srcOggVorbis[offset + 1] == 'v' && srcOggVorbis[offset + 2] == 'o' && srcOggVorbis[offset + 3] == 'r' && srcOggVorbis[offset + 4] == 'b' && srcOggVorbis[offset + 5] == 'i' && srcOggVorbis[offset + 6] == 's') {
                		final byte type = srcOggVorbis[offset];
                		offset += 6;
                		if(type == 1) { //Identification
                			vorbisstart[0] = offset;
                			vorbissize[0] = 23;
                    		offset += 23;
                		}
                		if(type == 3) { //Comment
                			vorbisstart[1] = offset;
                			long packetsize = 0;
                			long length = (srcOggVorbis[offset + 3] & 0xFF) << 24;
                			length |= (srcOggVorbis[offset + 2] & 0xFF) << 16;
                			length |= (srcOggVorbis[offset + 1] & 0xFF) << 8;
                			length |= (srcOggVorbis[offset] & 0xFF);
                			offset+=4;
                			offset+=length;
                			packetsize+=4;
                			packetsize+=length;
                			
                			byte lengthid = -1;
                			int count = 0;
                			while(++lengthid < 4) {
                    			count += srcOggVorbis[offset + lengthid] & 0xFF;
                			}
                			offset+=4;
                			packetsize+=4;
                			int i = -1;
                			while(++i > count) {
                				length = (srcOggVorbis[offset + 3] & 0xFF) << 24;
                    			length |= (srcOggVorbis[offset + 2] & 0xFF) << 16;
                    			length |= (srcOggVorbis[offset + 1] & 0xFF) << 8;
                    			length |= (srcOggVorbis[offset] & 0xFF);
                    			offset+=4;
                    			offset+=length;
                    			packetsize+=4;
                    			packetsize+=length;
                			}
                			++offset;
                			++packetsize;
                			packetsize &=0xFFFFFFFF;
                			vorbissize[1] = (int) packetsize;
                		}
                		if(type == 5) { //Setup
                			vorbisstart[2] = offset;
                			int packetsize = pagestarts[pageid-1]-offset; //lets hope that it ends on ogg page end
                			offset+= packetsize;
                			vorbissize[2] = packetsize;
                		}
                	} else {
                		++offset;
                	}
        		}
        	}
        }*/
        
        

        //final int vorbisHeaderEnd = vorbisstart[2] + vorbissize[2];
        final int vorbisHeaderEnd = pagestarts[pagecount-3]; //Lets hope that vorbis header in first 2 ogg pages
        byte[] firstpart = new byte[center - bof];
        byte[] secondpart = new byte[eof - center + vorbisHeaderEnd];
        System.arraycopy(srcOggVorbis, bof, firstpart, 0, firstpart.length);
        System.arraycopy(srcOggVorbis, center, secondpart, vorbisHeaderEnd, secondpart.length - vorbisHeaderEnd);
        System.arraycopy(srcOggVorbis, bof, secondpart, 0, vorbisHeaderEnd); //Copy vorbis headers to second part
        
        offset = pagesizes[0];
        /*byte i = 14;
        long length = 0;
        while(--i > 5) {
            length = srcOggVorbis[offset + i];
            length <<= 8;
        }
        
        length >>>= 1;*/
        offset = pagestarts[(pagecount >>> 1) + 1];
        //long alength = length;
        byte i = 14;
        while(--i > 5) {
        	firstpart[offset + i] = 0;
            //firstpart[offset + i] = (byte) (alength & 0xFF); 
            //alength >>>= 8;
        }
        firstpart[offset + 5] |= 0x04; //EOS
        
        offset = pagestarts[0] - center;
        //alength = length;
        i = 14;
        while(--i > 5) {
        	secondpart[offset + i] = 0;
        	//secondpart[offset + i] = (byte) (alength & 0xFF); 
            //alength >>>= 8;
        }
        
        //secondpart[5] |= 0x02; //BOS //Not needed since headers copied
        
        part1 = firstpart; //OK;
        part2 = secondpart; //OK; may not work if vorbis header not in 2 first ogg pages
	}
}
