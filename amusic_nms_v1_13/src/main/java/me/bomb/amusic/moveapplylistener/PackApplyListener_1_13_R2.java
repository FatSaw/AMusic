package me.bomb.amusic.moveapplylistener;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.server.v1_13_R2.EntityPlayer;

final class PackApplyListener_1_13_R2 extends PackApplyListener {
	
	@Override
	protected void addApplyListenTask(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		ChannelPipeline pipeline = entityplayer.playerConnection.networkManager.channel.pipeline();
		AtomicBoolean ab = new AtomicBoolean(true);
		pipeline.addBefore("packet_handler", "applylistener", new PacketMonitor_1_13_R2(ab));
		applylisteners.put(player.getUniqueId(), new AtomicBoolean[] {new AtomicBoolean(true),ab});
	}

	@Override
	protected void removeApplyListenTask(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		Channel channel = entityplayer.playerConnection.networkManager.channel;
		channel.eventLoop().submit(() -> {
			channel.pipeline().remove("applylistener");
			return null;
		});
		applylisteners.remove(player.getUniqueId());
	}

}
