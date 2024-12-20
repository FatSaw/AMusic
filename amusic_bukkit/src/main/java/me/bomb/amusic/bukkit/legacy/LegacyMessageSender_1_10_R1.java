package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import me.bomb.amusic.bukkit.MessageSender;
import net.minecraft.server.v1_10_R1.IChatBaseComponent;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import net.minecraft.server.v1_10_R1.PlayerConnection;
import net.minecraft.server.v1_10_R1.IChatBaseComponent.ChatSerializer;

public class LegacyMessageSender_1_10_R1 implements MessageSender {

	@Override
	public void send(CommandSender target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof Player) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent,(byte) 0));
			return;
		}
		target.sendMessage(chatbasecomponent.toPlainText());
	}

}
