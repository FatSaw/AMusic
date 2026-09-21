package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.MinecraftConnection;


public abstract class ConnectedPlayer implements Player {
	
	public MinecraftConnection getConnection() {
	    return null;
	}

}
