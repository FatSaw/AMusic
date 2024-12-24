package me.bomb.amusic.bukkit.legacy;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;

import me.bomb.amusic.MessageSender;
import net.minecraft.server.v1_10_R1.IChatBaseComponent;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import net.minecraft.server.v1_10_R1.PlayerConnection;
import net.minecraft.server.v1_10_R1.IChatBaseComponent.ChatSerializer;

public final class LegacyMessageSender_1_10_R1 implements MessageSender {

	@Override
	public void send(Object target, String message) {
		IChatBaseComponent chatbasecomponent = ChatSerializer.a(message);
		if(target instanceof CraftPlayer) {
			PlayerConnection playerconnection = ((CraftPlayer)target).getHandle().playerConnection;
			playerconnection.sendPacket(new PacketPlayOutChat(chatbasecomponent,(byte) 0));
			return;
		}
		((CommandSender)target).sendMessage(chatbasecomponent.toPlainText());
	}

	@Override
	public final String getLocale(Object target) {
		if(target instanceof CraftPlayer) {
			CraftPlayer player = (CraftPlayer)target;
			try {
				return player.getLocale().toLowerCase();
			} catch (NoSuchMethodError e) {
				return player.spigot().getLocale().toLowerCase();
			}
		}
		return null;
	}

}
