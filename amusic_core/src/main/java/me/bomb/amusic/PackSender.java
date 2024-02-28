package me.bomb.amusic;

import java.util.UUID;

public interface PackSender {
	
	public void send(UUID uuid, String url, byte[] sha1);
	
}
