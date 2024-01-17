package me.bomb.amusic;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.NetworkManager;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelPipeline;

final class PackApplyListener_1_7_R4 extends PackApplyListener {
	
	private final Field channelField;
	
	protected PackApplyListener_1_7_R4() throws NoSuchFieldException, SecurityException {
		channelField = NetworkManager.class.getDeclaredField("m");
		channelField.setAccessible(true);
	}
	
	@Override
	protected void addApplyListenTask(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		ChannelPipeline pipeline = null;
		try {
			Channel channel = (Channel) channelField.get(entityplayer.playerConnection.networkManager);
		    pipeline = channel.pipeline();
		} catch (IllegalArgumentException | IllegalAccessException e) {
		}
		if(pipeline==null) {
			return;
		}
		AtomicBoolean ab = new AtomicBoolean(true);
		pipeline.addBefore("packet_handler", "applylistener", new PacketMonitor_1_7_R4(ab));
		applylisteners.put(player.getUniqueId(), new AtomicBoolean[] {new AtomicBoolean(true),ab});
	}

	@Override
	protected void removeApplyListenTask(Player player) {
		EntityPlayer entityplayer = ((CraftPlayer)player).getHandle();
		Channel achannel = null;
		try {
			achannel = (Channel) channelField.get(entityplayer.playerConnection.networkManager);
		} catch (IllegalArgumentException | IllegalAccessException e) {
		}
		if(achannel==null) {
			return;
		}
		Channel channel = achannel;
		channel.eventLoop().submit(() -> {
			channel.pipeline().remove("applylistener");
			return null;
		});
		applylisteners.remove(player.getUniqueId());
	}

}
