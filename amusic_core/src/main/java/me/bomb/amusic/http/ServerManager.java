package me.bomb.amusic.http;

import java.net.InetAddress;
import java.util.Collection;

import javax.net.ServerSocketFactory;

public final class ServerManager {
	
	private final ServerWatcher serverwatcher;
	
	public ServerManager(final InetAddress ip, final int port, final int backlog, final int timeout, final ServerSocketFactory serverfactory, final Collection<InetAddress> onlineips, final ServerWorker worker, short connectcount) {
		ServerConnect[] connects = new ServerConnect[connectcount];
		this.serverwatcher = new ServerWatcher(ip, port, backlog, timeout, serverfactory, connects);
		if(onlineips == null) {
			while(--connectcount > -1) {
				connects[connectcount] = new ServerConnect(serverwatcher, worker);
			}
		} else {
			while(--connectcount > -1) {
				connects[connectcount] = new ConnectedServerConnect(serverwatcher, worker, onlineips);
			}
		}
	}
	
	public void start() {
		serverwatcher.start();
	}
	
	public void restart() {
		serverwatcher.restart();
	}
	
	public void end() {
		serverwatcher.end();
	}

}
