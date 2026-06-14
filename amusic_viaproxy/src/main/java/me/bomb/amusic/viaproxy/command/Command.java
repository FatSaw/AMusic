package me.bomb.amusic.viaproxy.command;

import com.viaversion.viaversion.api.connection.UserConnection;

public interface Command {
	
	public void handleConsole(org.apache.logging.log4j.Logger logger, String[] args);
	
	public void handlePlayer(UserConnection connection, String[] args);

}
