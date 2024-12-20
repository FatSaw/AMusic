package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.bukkit.MessageSender;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PlayerConnection;

public final class LegacyMessageSender_1_8_R3 implements MessageSender {

	@Override
	public void send(CommandSender target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof Player) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent,(byte) 0));
			return;
		}
		target.sendMessage(chatbasecomponent.c());
	}

}
