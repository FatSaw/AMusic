package me.bomb.amusic.viaproxy.command;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.util.Logger;

public interface Command {
	
	public void handleConsole(Logger logger, String[] args);
	
	public void handlePlayer(UserConnection connection, String[] args);

}
