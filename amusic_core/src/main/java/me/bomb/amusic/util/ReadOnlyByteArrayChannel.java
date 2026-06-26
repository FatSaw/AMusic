package me.bomb.amusic.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public final class ReadOnlyByteArrayChannel implements SeekableByteChannel {
	
	private int offset;
	private final byte[] data;

	public ReadOnlyByteArrayChannel(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("Data array cannot be null");
		}
		this.data = data;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int remaining = data.length - offset;
		if(remaining < 1) {
			return -1;
		}
		int dremaining = dst.remaining();
		if(dremaining < remaining) {
			remaining = dremaining;
		}
		dst.put(data, offset, remaining);
		offset += remaining;
		return remaining;
	}

	@Override
	public long position() throws IOException {
		return offset;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		if (newPosition < 0 || newPosition > data.length) {
			throw new IllegalArgumentException("Invalid position: " + newPosition);
		}
		offset = (int) newPosition;
		return this;
	}

	@Override
	public long size() throws IOException {
		return data.length;
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void close() {
	}

	@Override
	public int write(ByteBuffer src) {
		throw new NonWritableChannelException();
	}

	@Override
	public SeekableByteChannel truncate(long size) {
		throw new NonWritableChannelException();
	}
}
