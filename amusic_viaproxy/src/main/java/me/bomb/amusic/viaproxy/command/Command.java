package me.bomb.amusic.viaproxy.command;

import com.viaversion.viaversion.api.connection.UserConnection;

import me.bomb.amusic.util.AMusicLogger;

public interface Command {
	
	public void handleConsole(AMusicLogger logger, String[] args);
	
	public void handlePlayer(UserConnection connection, String[] args);

}
