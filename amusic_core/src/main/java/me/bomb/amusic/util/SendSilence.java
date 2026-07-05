package me.bomb.amusic.util;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

public final class SendSilence implements Runnable {

	// There is no sound stop packet below protocol version 110, it still can be
	// stopped by world rejoin or more than 4 sounds playing on the same time.
	// 4 silence sounds should be processed by client in different ticks

	private final static ByteBuf legacysoundstop1, legacysoundstop2;

	static {
		ByteBuf legacysoundstop;
		legacysoundstop = Unpooled.buffer(52, 52);
		legacysoundstop.writeByte(0x29);
		legacysoundstop.writeByte(33);
		legacysoundstop.writeBytes("minecraft:amusic.internal.silence".getBytes(StandardCharsets.US_ASCII));
		legacysoundstop.writeInt(0);
		legacysoundstop.writeInt(Integer.MIN_VALUE);
		legacysoundstop.writeInt(0);
		legacysoundstop.writeFloat(1.0E9f);
		legacysoundstop.writeByte(63);
		legacysoundstop1 = Unpooled.unreleasableBuffer(legacysoundstop);

		legacysoundstop = Unpooled.buffer(53, 53);
		legacysoundstop.writeByte(0x19);
		legacysoundstop.writeByte(33);
		legacysoundstop.writeBytes("minecraft:amusic.internal.silence".getBytes(StandardCharsets.US_ASCII));
		legacysoundstop.writeByte(0);
		legacysoundstop.writeInt(0);
		legacysoundstop.writeInt(Integer.MIN_VALUE);
		legacysoundstop.writeInt(0);
		legacysoundstop.writeFloat(1.0E9f);
		legacysoundstop.writeByte(63);
		legacysoundstop2 = Unpooled.unreleasableBuffer(legacysoundstop);
	}

	final Channel channel;
	final ByteBuf buf;
	final ChannelPromise voidpromise;
	final EventLoop eventloop;
	byte remaining;

	public SendSilence(final Channel channel, final int version, byte remaining) {
		ByteBuf buf;
		if (version < 0 || version > 110 || (buf = version < 48 ? legacysoundstop1 : version > 106 ? legacysoundstop2 : null) == null) {
			throw new IllegalStateException("Can not encode protocol ".concat(Integer.toString(version)));
		}
		this.channel = channel;
		this.buf = buf.retainedDuplicate();
		this.remaining = remaining;
		this.voidpromise = channel.voidPromise();
		this.eventloop = channel.eventLoop();
	}

	@Override
	public void run() {
		if (!this.channel.isActive()) {
			return;
		}
		this.buf.readerIndex(0);
		this.channel.writeAndFlush(this.buf, this.voidpromise);
		if(--this.remaining < 0) {
			return;
		}
		this.eventloop.schedule(this, 50L, TimeUnit.MILLISECONDS);
	}

}
