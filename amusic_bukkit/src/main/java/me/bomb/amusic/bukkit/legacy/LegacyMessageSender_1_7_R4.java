package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;

import me.bomb.amusic.MessageSender;
import net.minecraft.server.v1_7_R4.IChatBaseComponent;
import net.minecraft.server.v1_7_R4.PacketPlayOutChat;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.server.v1_7_R4.ChatSerializer;

public final class LegacyMessageSender_1_7_R4 implements MessageSender {

	@Override
	public void send(Object target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof CraftPlayer) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent, false));
			return;
		}
		((CommandSender)target).sendMessage(chatbasecomponent.c());
	}
	
	@Override
	public final String getLocale(Object target) {
		if(target instanceof CraftPlayer) {
			CraftPlayer player = (CraftPlayer)target;
			return player.getHandle().locale;
		}
		return null;
	}

}
