package me.bomb.amusic.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public final class ZipOutput {

	private static final int LOC_SIG = 0x04034b50;
	private static final int CEN_SIG = 0x02014b50;
	private static final int END_SIG = 0x06054b50;

	private CountingOutputStream outstream;
	private List<ZipEntryHeader> entries = new ArrayList<>();

	public ZipOutput(OutputStream outstream) {
		this.outstream = new CountingOutputStream(outstream);
	}
	
	public void putEntry(String localname, byte[] data, String name1) throws IOException {
		this.putEntry(localname, data, 0, data.length, new String[] {name1});
	}
	
	public void putEntry(String localname, byte[] data, String name1, String name2) throws IOException {
		this.putEntry(localname, data, 0, data.length, new String[] {name1, name2});
	}

	public void putEntry(String localname, byte[] data, int off, int len, String[] names) throws IOException {
		long offset = this.outstream.getByteCount();
		int crc = calculateCRC32(data, off, len);
		this.writeLocalHeader(localname, crc, data.length);
		this.outstream.write(data, off, len);
		int i = names.length;
		while(--i > -1) {
			this.entries.add(new ZipEntryHeader(names[i], crc, data.length, offset));
		}
	}

	private void writeLocalHeader(String name, int crc, int size) throws IOException {
		byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
		writeInt(this.outstream, LOC_SIG);
		writeShort(this.outstream, 20);
		writeShort(this.outstream, 0);
		writeShort(this.outstream, 0);
		writeShort(this.outstream, 0);
		writeShort(this.outstream, 0);
		writeInt(this.outstream, crc);
		writeInt(this.outstream, size);
		writeInt(this.outstream, size);
		writeShort(this.outstream, nameBytes.length);
		writeShort(this.outstream, 0);
		this.outstream.write(nameBytes);
	}

	public void writeCentralDirectory() throws IOException {
		long cdOffset = this.outstream.getByteCount();
		long cdSize = 0;

		for (ZipEntryHeader entry : entries) {
			long startHeader = this.outstream.getByteCount();
			byte[] nameBytes = entry.name.getBytes(StandardCharsets.UTF_8);

			writeInt(this.outstream, CEN_SIG);
			writeShort(this.outstream, 20);
			writeShort(this.outstream, 20);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeInt(this.outstream, entry.crc32);
			writeInt(this.outstream, entry.size);
			writeInt(this.outstream, entry.size);
			writeShort(this.outstream, nameBytes.length);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeShort(this.outstream, 0);
			writeInt(this.outstream, 0);
			writeInt(this.outstream, (int) entry.offset);
			this.outstream.write(nameBytes);
			cdSize += (this.outstream.getByteCount() - startHeader);
		}
		writeInt(this.outstream, END_SIG);
		writeShort(this.outstream, 0);
		writeShort(this.outstream, 0);
		writeShort(this.outstream, entries.size());
		writeShort(this.outstream, entries.size());
		writeInt(this.outstream, (int) cdSize);
		writeInt(this.outstream, (int) cdOffset);
		writeShort(this.outstream, 0);
	}

	private static void writeShort(OutputStream out, int v) throws IOException {
		out.write((v >>> 0) & 0xFF);
		out.write((v >>> 8) & 0xFF);
	}

	private static void writeInt(OutputStream out, int v) throws IOException {
		out.write((v >>> 0) & 0xFF);
		out.write((v >>> 8) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>> 24) & 0xFF);
	}

	private static int calculateCRC32(byte[] data, int off, int len) {
		CRC32 crc = new CRC32();
		crc.update(data, off, len);
		return (int) crc.getValue();
	}

	private static class ZipEntryHeader {
		protected String name;
		protected int crc32;
		protected int size;
		protected long offset;

		protected ZipEntryHeader(String name, int crc32, int size, long offset) {
			if (name == null) throw new IllegalArgumentException();
			this.name = name;
			this.crc32 = crc32;
			this.size = size;
			this.offset = offset;
		}
	}

	private static class CountingOutputStream extends OutputStream {
		private final OutputStream out;
		private long count = 0;

		CountingOutputStream(OutputStream out) {
			this.out = out;
		}

		public long getByteCount() {
			return count;
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
			++count;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			count += len;
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}
	}
}