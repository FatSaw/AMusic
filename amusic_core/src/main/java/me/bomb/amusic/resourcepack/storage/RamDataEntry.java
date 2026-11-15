package me.bomb.amusic.resourcepack.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RamDataEntry extends DataEntry {

	private final ByteBuffer pack;
	
	protected RamDataEntry(String storeid, ResourcepackInfoImpl info, ByteBuffer pack) {
		super(storeid, info);
		this.pack = pack;
	}

	@Override
	public ByteBuffer getPack() {
		return this.pack;
	}
	
	@Override
	public void sendTo(SocketChannel channel) throws IOException {
		channel.write(this.getPack());
	}
	
	@Override
	public int getLength() {
		return this.info.packsize;
	}

}
