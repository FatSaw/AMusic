package me.bomb.amusic.sponge;


import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;

import me.bomb.amusic.MessageSender;

public final class SpongeMessageSender implements MessageSender {
	
	private final TextSerializer serializer;
	
	protected SpongeMessageSender() {
		this.serializer = TextSerializers.JSON;
	}

	@Override
	public final void send(Object target, String message) {
		if(target instanceof Player) {
			Player player = (Player) target;
			player.sendMessage(ChatTypes.SYSTEM, this.serializer.deserialize(message));
		} else if(target instanceof CommandSource) {
			((CommandSource)target).sendMessage(this.serializer.deserialize(message));
		}
	}
	
	@Override
	public final String getLocale(Object target) {
		if(target instanceof Player) {
			Player player = (Player) target;
			return player.getLocale().toString().toLowerCase();
		}
		return null;
	}

}
