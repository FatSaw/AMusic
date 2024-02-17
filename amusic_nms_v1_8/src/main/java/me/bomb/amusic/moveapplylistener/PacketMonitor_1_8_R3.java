package me.bomb.amusic.moveapplylistener;

import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying.PacketPlayInPosition;

final class PacketMonitor_1_8_R3 extends ChannelDuplexHandler {
	private final AtomicBoolean applied;
	protected PacketMonitor_1_8_R3(AtomicBoolean applied) {
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
