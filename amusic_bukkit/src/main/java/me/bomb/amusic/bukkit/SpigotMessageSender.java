package me.bomb.amusic.bukkit;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class SpigotMessageSender implements MessageSender {

	@Override
	public void send(CommandSender target, String message) {
		BaseComponent[] bcmessage = ComponentSerializer.parse(message);
		target.spigot().sendMessage(bcmessage);
	}

}
