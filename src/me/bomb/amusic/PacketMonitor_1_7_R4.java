package me.bomb.amusic;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.server.v1_7_R4.PacketPlayInPosition;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;

final class PacketMonitor_1_7_R4 extends ChannelDuplexHandler {
	private final AtomicBoolean applied;
	protected PacketMonitor_1_7_R4(AtomicBoolean applied) {
		this.applied = applied;
	}
	@Override
	public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {
		super.channelRead(context, packet);
		if (packet instanceof PacketPlayInPosition) {
			applied.set(true);
		}
	}
}
