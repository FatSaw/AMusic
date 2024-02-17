package me.bomb.amusic.legacy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class LegacyPackSender {
	
	protected abstract void sendPack(Player player,String url,String sha1);
	
}
