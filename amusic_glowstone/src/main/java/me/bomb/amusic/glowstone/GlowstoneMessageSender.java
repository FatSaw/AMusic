package me.bomb.amusic.glowstone;

import org.bukkit.command.CommandSender;

import me.bomb.amusic.MessageSender;
import net.glowstone.entity.GlowPlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;

public final class GlowstoneMessageSender implements MessageSender {
	
	protected GlowstoneMessageSender() {
	}

	@Override
	public final void send(Object target, String message) {
		if(target instanceof GlowPlayer) {
			GlowPlayer player = (GlowPlayer) target;
			player.sendMessage(ChatMessageType.SYSTEM, ComponentSerializer.parse(message));
		} else if(target instanceof CommandSender) {
			((CommandSender)target).sendMessage(ComponentSerializer.parse(message));
		}
	}
	
	@Override
	public final String getLocale(Object target) {
		if(target instanceof GlowPlayer) {
			GlowPlayer player = (GlowPlayer) target;
			return player.getLocale().toLowerCase();
		}
		return null;
	}

}
