package me.bomb.amusic.velocity;

import java.util.Locale;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.MessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;

public final class VelocityMessageSender implements MessageSender {
	
	private final JSONComponentSerializer serializer;
	
	protected VelocityMessageSender() {
		serializer = JSONComponentSerializer.json();
	}

	@Override
	public void send(Object target, String message) {
		if(target instanceof CommandSource) {
			Component component = serializer.deserialize(message);
			if(component != null) {
				((CommandSource)target).sendMessage(component);
				return;
			}
		}
	}
	
	@Override
	public String getLocale(Object target) {
		if(target instanceof Player) {
			Locale locale = ((Player)target).getPlayerSettings().getLocale();
			if(locale==null) {
				return "default";
			}
			StringBuilder sb = new StringBuilder();
			sb.append(locale.getLanguage());
			sb.append("_");
			sb.append(locale.getCountry());
			return sb.toString().toLowerCase();
		}
		return null;
	}

}
