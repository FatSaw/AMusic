package me.bomb.amusic;

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_9_R2.PacketPlayInFlying.PacketPlayInPosition;

final class PacketMonitor_1_9_R2 extends ChannelDuplexHandler {
	private final AtomicBoolean applied;
	protected PacketMonitor_1_9_R2(AtomicBoolean applied) {
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