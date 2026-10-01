package me.bomb.amusic.resourcepack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RamDataEntry extends DataEntry {

	private final byte[] pack;
	
	protected RamDataEntry(String storeid, ResourcepackInfoImpl info, byte[] pack) {
		super(storeid, info);
		this.pack = pack;
	}

	@Override
	public byte[] getPack() {
		return this.pack;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		channel.write(ByteBuffer.wrap(this.getPack()));
	}
	
	@Override
	public int getLength() {
		return this.info.packsize;
	}

}
