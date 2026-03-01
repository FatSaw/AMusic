package me.bomb.amusic.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunked output stream optimized for large ZIP archives.
 *
 * - Fixed-size chunks avoid repeated copying
 * - Memory usage remains stable during writes
 * - Final byte[] is created only once when requested
 */
public class ChunkedOutputStream extends OutputStream {

    private static final int DEFAULT_CHUNK_SIZE = 4194304;

    private final int chunkSize;
    private final List<byte[]> chunks = new ArrayList<>();

    private byte[] current;
    private int position;
    private int totalSize;

    public ChunkedOutputStream() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public ChunkedOutputStream(int chunkSize) {
        this.chunkSize = chunkSize;
        allocateChunk();
    }

    private void allocateChunk() {
        current = new byte[chunkSize];
        chunks.add(current);
        position = 0;
    }

    @Override
    public void write(int b) {
        if (position == chunkSize) allocateChunk();
        current[position++] = (byte) b;
        totalSize++;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        while (len > 0) {
            if (position == chunkSize) allocateChunk();
            int space = chunkSize - position;
            int toWrite = Math.min(space, len);
            System.arraycopy(b, off, current, position, toWrite);
            position += toWrite;
            off += toWrite;
            len -= toWrite;
            totalSize += toWrite;
        }
    }

    public int size() {
        return totalSize;
    }

    public void writeTo(OutputStream out) throws IOException {
        int remaining = totalSize;
        for (byte[] chunk : chunks) {
            int len = Math.min(chunkSize, remaining);
            out.write(chunk, 0, len);
            remaining -= len;
            if (remaining <= 0) break;
        }
    }

    public byte[] toByteArray() {
        byte[] result = new byte[totalSize];
        int pos = 0;
        int remaining = totalSize;
        for (byte[] chunk : chunks) {
            int len = Math.min(chunkSize, remaining);
            System.arraycopy(chunk, 0, result, pos, len);
            pos += len;
            remaining -= len;
            if (remaining <= 0) break;
        }
        return result;
    }

    public void reset() {
        chunks.clear();
        totalSize = 0;
        allocateChunk();
    }
}
