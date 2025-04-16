package me.bomb.amusic.util;

import java.io.IOException;
import java.io.OutputStream;

import static java.lang.System.arraycopy;


/**
 * This class implements an output stream in which the data is
 * written into a byte array array. The buffer automatically grows as data
 * is written to it.
 * The data can be retrieved using {@code toByteArray()} and
 * {@code toByteArrays()}.
 * <p>
 * Closing a {@code ByteArraysOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 */

public class ByteArraysOutputStream extends OutputStream {

    protected byte[][] bufs;

    protected int capacity = 0, index = 0;
    
    public ByteArraysOutputStream() {
    	bufs = new byte[capacity][];
    }

    public ByteArraysOutputStream(int capacity) {
    	this.capacity = capacity;
        bufs = new byte[capacity][];
    }
    
    private void increaseCapacity() {
    	if(index < capacity) {
    		return;
    	}
    	++capacity;
    	byte[][] bufs = this.bufs;
    	this.bufs = new byte[capacity][];
    	arraycopy(bufs, 0, this.bufs, 0, bufs.length);
    	
    }
    
    public synchronized void increaseCapacity(int count) {
    	capacity+=count;
    	byte[][] bufs = this.bufs;
    	this.bufs = new byte[capacity][];
    	arraycopy(bufs, 0, this.bufs, 0, bufs.length);
    }
    
    public synchronized void ensureCapacity(int count) {
    	int remain = capacity - index;
    	if(remain > count) {
    		return;
    	}
    	capacity+=count-remain;
    	byte[][] bufs = this.bufs;
    	this.bufs = new byte[capacity][];
    	arraycopy(bufs, 0, this.bufs, 0, bufs.length);
    }
    
    /**
     * Inefficient, should be avoided
     */
    @Override
    public synchronized void write(int b) {
    	increaseCapacity();
    	byte[] buf = new byte[] {(byte) b};
    	bufs[index] = buf;
        ++index;
    }
    
    /**
     * Buffer should not be reused
     */
    @Override
    public synchronized void write(byte buf[]) {
    	increaseCapacity();
    	bufs[index] = buf;
        ++index;
    }

    /**
     * Inefficient, should be avoided
     */
    @Override
    public synchronized void write(byte b[], int off, int len) {
    	increaseCapacity();
    	byte[] buf = new byte[len - off];
        arraycopy(b, off, buf, 0, buf.length);
    	bufs[index] = buf;
        ++index;
    }

    public synchronized void writeBytes(byte buf[]) {
        write(buf, 0, buf.length);
    }
    
    public synchronized void writeTo(OutputStream out) throws IOException {
    	final byte[][] bufs = this.bufs;
    	final int index = this.index;
    	for(int i = 0;i < index;++i) {
            out.write(bufs[i], 0, bufs[i].length);
    	}
    }

    public synchronized void reset() {
    	index = 0;
    }
    
    public synchronized byte[][] toByteArrays() {
    	int i = this.index;
    	byte[][] bufs = new byte[i][];
    	while(--i > -1) {
    		bufs[i] = this.bufs[i];
    	}
        return bufs;
    }
    
    public synchronized byte[] toByteArray() {
    	final byte[][] bufs = this.bufs;
    	final int index = this.index;
    	int i = index,pos = 0;
    	while(--i > -1) {
    		pos += bufs[i].length;
    	}
    	byte[] buf = new byte[pos];
    	i = index;
    	while(--i > -1) {
    		byte[] src = bufs[i];
    		pos-=src.length;
    		arraycopy(src, 0, buf, pos, src.length);
    	}
        return buf;
    }
    
    public synchronized int size() {
    	final byte[][] bufs = this.bufs;
    	int i = this.index, size = 0;
    	while(--i > -1) {
    		size += bufs[i].length;
    	}
    	return size;
    }
    
    public synchronized int index() {
        return index;
    }

    public synchronized int capacity() {
        return capacity;
    }
    
    /**
     * Closing a {@code ByteArraysOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     */
    @Override
    public void close() throws IOException {
    }

}