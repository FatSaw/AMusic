package me.bomb.amusic.bukkit;

import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.MessageSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public final class SpigotMessageSender implements MessageSender {
	
	protected SpigotMessageSender() {
	}

	@Override
	public final void send(Object target, String message) {
		if(target instanceof BlockCommandSender) {
			
		} else if(target instanceof CommandSender) {
			BaseComponent[] bcmessage = ComponentSerializer.parse(message);
			((CommandSender)target).spigot().sendMessage(bcmessage);
		}
	}
	
	@Override
	public final String getLocale(Object target) {
		if(target instanceof Player) {
			Player player = (Player)target;
			try {
				return player.getLocale().toLowerCase();
			} catch (NoSuchMethodError e) {
				return player.spigot().getLocale().toLowerCase();
			}
		}
		return null;
	}

}
