package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.bukkit.MessageSender;
import net.minecraft.server.v1_7_R4.IChatBaseComponent;
import net.minecraft.server.v1_7_R4.PacketPlayOutChat;
import net.minecraft.server.v1_7_R4.PlayerConnection;
import net.minecraft.server.v1_7_R4.ChatSerializer;

public final class LegacyMessageSender_1_7_R4 implements MessageSender {

	@Override
	public void send(CommandSender target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof Player) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent, false));
			return;
		}
		target.sendMessage(chatbasecomponent.c());
	}

}
