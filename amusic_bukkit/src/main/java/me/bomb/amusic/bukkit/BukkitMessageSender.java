package me.bomb.amusic.bukkit;

import org.bukkit.command.CommandSender;

public final class BukkitMessageSender implements MessageSender {
	
	protected BukkitMessageSender() {
	}

	@Override
	public void send(CommandSender target, String message) {
		target.sendMessage(message);
	}

}
