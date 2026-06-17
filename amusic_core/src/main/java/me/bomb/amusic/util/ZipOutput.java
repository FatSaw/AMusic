package me.bomb.amusic.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public final class ZipOutput {

	private OutputStream outstream;
	private int count = 0;
	private int entrycount = 0;
	private final List<byte[]> bentries;
	private final Deflater deflater;
	private final CRC32 acrc;

	public ZipOutput(OutputStream outstream) {
		this.outstream = outstream;
		this.bentries = new ArrayList<>();
		this.deflater = new Deflater(9, true);
		this.acrc = new CRC32();
	}
	
	public void putEntry(byte[] data, String name) throws IOException {
		long offset = count;
		acrc.update(data);
		int crc = (int) acrc.getValue(), ulen = data.length;
		acrc.reset();
		deflater.setInput(data);
		deflater.finish();
		
		int i = (ulen >>> 12) + 3;
		int clen = 0, read;
		byte[][] bbuf = new byte[i][];
		int[] bsizes = new int[i];
		byte[] buf;
		while(--i > -1 && (read = deflater.deflate(buf = new byte[4096])) != 0) {
			bbuf[i] = buf;
			bsizes[i] = read;
			clen += read;
		}
		deflater.reset();
		buf = null;
		byte[] localHeader = new byte[] {0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		localHeader[14] = (byte) (crc & 0xFF);
		localHeader[15] = (byte) ((crc >>> 8) & 0xFF);
		localHeader[16] = (byte) ((crc >>> 16) & 0xFF);
		localHeader[17] = (byte) ((crc >>> 24) & 0xFF);
		localHeader[18] = (byte) (clen & 0xFF);
		localHeader[19] = (byte) ((clen >>> 8) & 0xFF);
		localHeader[20] = (byte) ((clen >>> 16) & 0xFF);
		localHeader[21] = (byte) ((clen >>> 24) & 0xFF);
		localHeader[22] = (byte) (ulen & 0xFF);
		localHeader[23] = (byte) ((ulen >>> 8) & 0xFF);
		localHeader[24] = (byte) ((ulen >>> 16) & 0xFF);
		localHeader[25] = (byte) ((ulen >>> 24) & 0xFF);
		this.outstream.write(localHeader);
		count+=localHeader.length;
		int j = bbuf.length;
		while(--j > i) {
			this.outstream.write(bbuf[j], 0, bsizes[j]);
		}
		count+=clen;
		byte[] nameb = name.getBytes(StandardCharsets.US_ASCII);
		int namel = nameb.length;
		byte[] globalHeaderEntry = new byte[] {0x50, 0x4b, 0x01, 0x02, 0x14, 0x00, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		globalHeaderEntry[16] = (byte) (crc & 0xFF);
		globalHeaderEntry[17] = (byte) ((crc >>> 8) & 0xFF);
		globalHeaderEntry[18] = (byte) ((crc >>> 16) & 0xFF);
		globalHeaderEntry[19] = (byte) ((crc >>> 24) & 0xFF);
		globalHeaderEntry[20] = (byte) (clen & 0xFF);
		globalHeaderEntry[21] = (byte) ((clen >>> 8) & 0xFF);
		globalHeaderEntry[22] = (byte) ((clen >>> 16) & 0xFF);
		globalHeaderEntry[23] = (byte) ((clen >>> 24) & 0xFF);
		globalHeaderEntry[24] = (byte) (ulen & 0xFF);
		globalHeaderEntry[25] = (byte) ((ulen >>> 8) & 0xFF);
		globalHeaderEntry[26] = (byte) ((ulen >>> 16) & 0xFF);
		globalHeaderEntry[27] = (byte) ((ulen >>> 24) & 0xFF);
		globalHeaderEntry[28] = (byte) (namel & 0xFF);
		globalHeaderEntry[29] = (byte) ((namel >>> 8) & 0xFF);
		globalHeaderEntry[42] = (byte) (offset & 0xFF);
		globalHeaderEntry[43] = (byte) ((offset >>> 8) & 0xFF);
		globalHeaderEntry[44] = (byte) ((offset >>> 16) & 0xFF);
		globalHeaderEntry[45] = (byte) ((offset >>> 24) & 0xFF);
		bentries.add(globalHeaderEntry);
		bentries.add(nameb);
		++entrycount;
	}

	public void putSound(byte[] data, String javapath, String bedrockpath) throws IOException {
		long offset = count;
		acrc.update(data);
		int crc = (int) acrc.getValue(), len = data.length;
		acrc.reset();
		byte[] localHeader = new byte[] {0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		localHeader[14] = (byte) (crc & 0xFF);
		localHeader[15] = (byte) ((crc >>> 8) & 0xFF);
		localHeader[16] = (byte) ((crc >>> 16) & 0xFF);
		localHeader[17] = (byte) ((crc >>> 24) & 0xFF);
		localHeader[18] = (byte) (len & 0xFF);
		localHeader[19] = (byte) ((len >>> 8) & 0xFF);
		localHeader[20] = (byte) ((len >>> 16) & 0xFF);
		localHeader[21] = (byte) ((len >>> 24) & 0xFF);
		localHeader[22] = (byte) (len & 0xFF);
		localHeader[23] = (byte) ((len >>> 8) & 0xFF);
		localHeader[24] = (byte) ((len >>> 16) & 0xFF);
		localHeader[25] = (byte) ((len >>> 24) & 0xFF);
		this.outstream.write(localHeader);
		count+=localHeader.length;
		this.outstream.write(data);
		count+=data.length;
		byte[] jnameb = javapath.getBytes(StandardCharsets.US_ASCII), bnameb = bedrockpath.getBytes(StandardCharsets.US_ASCII), jglobalHeaderEntry = new byte[] {0x50, 0x4b, 0x01, 0x02, 0x14, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, bglobalHeaderEntry;
		int jnamel = jnameb.length, bnamel = bnameb.length;
		jglobalHeaderEntry[16] = (byte) (crc & 0xFF);
		jglobalHeaderEntry[17] = (byte) ((crc >>> 8) & 0xFF);
		jglobalHeaderEntry[18] = (byte) ((crc >>> 16) & 0xFF);
		jglobalHeaderEntry[19] = (byte) ((crc >>> 24) & 0xFF);
		jglobalHeaderEntry[20] = (byte) (len & 0xFF);
		jglobalHeaderEntry[21] = (byte) ((len >>> 8) & 0xFF);
		jglobalHeaderEntry[22] = (byte) ((len >>> 16) & 0xFF);
		jglobalHeaderEntry[23] = (byte) ((len >>> 24) & 0xFF);
		jglobalHeaderEntry[24] = (byte) (len & 0xFF);
		jglobalHeaderEntry[25] = (byte) ((len >>> 8) & 0xFF);
		jglobalHeaderEntry[26] = (byte) ((len >>> 16) & 0xFF);
		jglobalHeaderEntry[27] = (byte) ((len >>> 24) & 0xFF);
		jglobalHeaderEntry[42] = (byte) (offset & 0xFF);
		jglobalHeaderEntry[43] = (byte) ((offset >>> 8) & 0xFF);
		jglobalHeaderEntry[44] = (byte) ((offset >>> 16) & 0xFF);
		jglobalHeaderEntry[45] = (byte) ((offset >>> 24) & 0xFF);
		bglobalHeaderEntry = new byte[46];
		System.arraycopy(jglobalHeaderEntry, 0, bglobalHeaderEntry, 0, 46);
		jglobalHeaderEntry[28] = (byte) (jnamel & 0xFF);
		jglobalHeaderEntry[29] = (byte) ((jnamel >>> 8) & 0xFF);
		bglobalHeaderEntry[28] = (byte) (bnamel & 0xFF);
		bglobalHeaderEntry[29] = (byte) ((bnamel >>> 8) & 0xFF);
		bentries.add(jglobalHeaderEntry);
		bentries.add(jnameb);
		bentries.add(bglobalHeaderEntry);
		bentries.add(bnameb);
		++entrycount;
		++entrycount;
	}

	public void writeCentralDirectory() throws IOException {
		int cdOffset = count;
		int cdSize = 0;
		for(byte[] buf : bentries) {
			this.outstream.write(buf);
			cdSize += buf.length;
		}
		count+=cdSize;
		byte[] end = new byte[] {0x50, 0x4b, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		end[8] = (byte) (entrycount & 0xFF);
		end[9] = (byte) ((entrycount >>> 8) & 0xFF);
		end[10] = (byte) (entrycount & 0xFF);
		end[11] = (byte) ((entrycount >>> 8) & 0xFF);
		end[12] = (byte) (cdSize & 0xFF);
		end[13] = (byte) ((cdSize >>> 8) & 0xFF);
		end[14] = (byte) ((cdSize >>> 16) & 0xFF);
		end[15] = (byte) ((cdSize >>> 24) & 0xFF);
		end[16] = (byte) (cdOffset & 0xFF);
		end[17] = (byte) ((cdOffset >>> 8) & 0xFF);
		end[18] = (byte) ((cdOffset >>> 16) & 0xFF);
		end[19] = (byte) ((cdOffset >>> 24) & 0xFF);
		this.outstream.write(end);
		count+=end.length;
		deflater.end();
	}
}