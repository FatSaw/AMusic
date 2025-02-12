package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import me.bomb.amusic.MessageSender;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PlayerConnection;

public final class LegacyMessageSender_1_8_R3 implements MessageSender {

	@Override
	public void send(Object target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof CraftPlayer) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent,(byte) 0));
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
