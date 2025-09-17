package me.bomb.amusic.source;

import me.bomb.amusic.util.HexUtils;

public class OggVorbisPageInfo {
	
	public final int[] pagestarts, pagesizes;
	public final int vorbisInfoStart, vorbisCommentsStart, vorbisSetupStart; 
	public final int vorbisInfoSize, vorbisCommentsSize, vorbisSetupSize; 
	
	public OggVorbisPageInfo(final byte[] src) throws IllegalArgumentException {
		int offset = 0;
		int pagecount = 0;
        while (offset < src.length) {
            if (src[offset] == 'O' && src[offset + 1] == 'g' && src[offset + 2] == 'g' && src[offset + 3] == 'S') {
                try {
                	offset += 26;
                    int segmentCount = src[offset] & 0xFF;
                    int size = 0;
                    while(--segmentCount > -1) {
                    	int segmentSize = src[++offset] & 0xFF;
                    	size += segmentSize;
                    }
                    offset += size;
                    if(offset > src.length) {
                        continue;
                    }
                    ++pagecount;
                } catch (ArrayIndexOutOfBoundsException e) {
                	e.printStackTrace();
                }
            } else {
                ++offset;
            }
        }
        int[] pagestarts = new int[pagecount];
        int[] pagesizes = new int[pagecount];
        offset = 0;
        while (offset < src.length) {
            if (src[offset] == 'O' && src[offset + 1] == 'g' && src[offset + 2] == 'g' && src[offset + 3] == 'S') {
                if(--pagecount > -1) {
                    pagestarts[pagecount] = offset;
                    offset+=26;
                    try {
                        int segmentCount = src[offset] & 0xFF;
                        int size = 27 + segmentCount, segmentsSize = 0;
                        while(--segmentCount > -1) {
                        	int segmentSize = src[++offset] & 0xFF;
                        	segmentsSize += segmentSize;
                        }
                    	size += segmentsSize;
                    	offset += segmentsSize;
                        pagesizes[pagecount] = size;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        pagesizes[pagecount] = src.length - pagestarts[pagecount];
                    }
                } else {
                    offset=Integer.MAX_VALUE;
                }
            } else {
                ++offset;
            }
        }
        this.pagestarts = pagestarts;
        this.pagesizes = pagesizes;
        
        pagecount = pagestarts.length;
        
        int[] vorbisstart = new int[3], vorbissize = new int[3];
        {
        	//SEARCH VORBIS HEADERS
        	int pageid = pagecount;
    		int end = 0;
        	while(--pageid > -1 && (vorbisstart[0] == 0 || vorbisstart[1] == 0 || vorbisstart[2] == 0)) {
        		int start = pagestarts[pageid];
        		offset = start;
        		offset+=26;
        		int segmentCount = src[offset] & 0xFF;

        		int segmentId = offset;
        		++offset;
        		offset+=segmentCount;
        		int segmentSize = 0;
        		while(--segmentCount > -1) {
        			segmentSize = src[++segmentId] & 0xFF;
        			end += segmentSize;
        			if(segmentSize != 255) {
                        if (src[offset + 1] == 'v' && src[offset + 2] == 'o' && src[offset + 3] == 'r' && src[offset + 4] == 'b' && src[offset + 5] == 'i' && src[offset + 6] == 's') {
                    		byte type = src[offset];
                    		if(type == 1 || type == 3 || type == 5) {
                    			type >>>= 1;
                    			vorbisstart[type] = offset;
                    			vorbissize[type] = end;
                    		}
                    		offset += end;
                    	}
                        end = 0;
        			}
        		}
        		if (segmentSize == 255) {
        			
        		}
        	}
        }
        vorbisInfoStart = vorbisstart[0];
        vorbisInfoSize = vorbissize[0];
        vorbisCommentsStart = vorbisstart[1];
        vorbisCommentsSize = vorbissize[1];
        vorbisSetupStart = vorbisstart[2];
        vorbisSetupSize = vorbissize[2];
	}
	
	@Override
	public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("vorbisInfo: ");
			sb.append(HexUtils.intToHex(vorbisInfoStart));
    		sb.append(':');
			sb.append(HexUtils.intToHex(vorbisInfoSize));
			sb.append('\n');

			sb.append("vorbisComments: ");
			sb.append(HexUtils.intToHex(vorbisCommentsStart));
    		sb.append(':');
			sb.append(HexUtils.intToHex(vorbisCommentsSize));
			sb.append('\n');

			sb.append("vorbisSetup: ");
			sb.append(HexUtils.intToHex(vorbisSetupStart));
    		sb.append(':');
			sb.append(HexUtils.intToHex(vorbisSetupSize));
			sb.append('\n');
			
			int i = this.pagestarts.length;
        	sb.append("pages (");
        	sb.append(i);
        	sb.append("): \n");
        	while(--i > -1) {
        		sb.append(HexUtils.intToHex(i));
        		sb.append(':');
        		sb.append(" start:");
        		sb.append(HexUtils.intToHex(pagestarts[i]));
        		sb.append(" size:");
        		sb.append(HexUtils.intToHex(pagesizes[i]));
        		sb.append(" end:");
        		sb.append(HexUtils.intToHex(pagestarts[i] + pagesizes[i]));
        		sb.append('\n');
        	}
        	return sb.toString();
        }
}
