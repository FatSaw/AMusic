package me.bomb.amusic;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.server.v1_11_R1.EntityPlayer;

final class PackApplyListener_1_11_R1 extends PackApplyListener {
	
	@Override
	protected void addApplyListenTask(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		ChannelPipeline pipeline = entityplayer.playerConnection.networkManager.channel.pipeline();
		AtomicBoolean ab = new AtomicBoolean(true);
		pipeline.addBefore("packet_handler", "applylistener", new PacketMonitor_1_11_R1(ab));
		applylisteners.put(player.getUniqueId(), ab);
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
