package me.bomb.amusic.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public final class ReadOnlyByteArrayChannel implements SeekableByteChannel {
	private final ByteBuffer buffer;
	private boolean open = true;

	public ReadOnlyByteArrayChannel(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("Data array cannot be null");
		}
		this.buffer = ByteBuffer.wrap(data).asReadOnlyBuffer();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		ensureOpen();
		if (!buffer.hasRemaining()) {
			return -1;
		}
		int bytesToRead = Math.min(dst.remaining(), buffer.remaining());
		ByteBuffer slice = buffer.slice();
		slice.limit(bytesToRead);
		dst.put(slice);
		buffer.position(buffer.position() + bytesToRead);
		return bytesToRead;
	}

	@Override
	public long position() throws IOException {
		ensureOpen();
		return buffer.position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		ensureOpen();
		if (newPosition < 0 || newPosition > buffer.limit()) {
			throw new IllegalArgumentException("Invalid position: " + newPosition);
		}
		buffer.position((int) newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		ensureOpen();
		return buffer.limit();
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() {
		open = false;
	}

	@Override
	public int write(ByteBuffer src) {
		throw new NonWritableChannelException();
	}

	@Override
	public SeekableByteChannel truncate(long size) {
		throw new NonWritableChannelException();
	}

	private void ensureOpen() throws ClosedChannelException {
		if (!open) {
			throw new ClosedChannelException();
		}
	}
}
