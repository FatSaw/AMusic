package me.bomb.amusic;

public interface MessageSender<T> {

	/**
	 * Sends message to player.
	 */
	public void send(T target, String message);
	
}
