package me.bomb.amusic.http;

import java.net.InetAddress;
import java.util.Collection;

import javax.net.ServerSocketFactory;

public final class ServerManager {
	
	private final InetAddress ip;
	private final int port;
	private final int backlog;
	private final int timeout;
	private final ServerSocketFactory serverfactory;
	private final Collection<InetAddress> onlineips;
	private final ServerWorker worker;
	private final short connectcount;
	
	private ServerWatcher serverwatcher;
	
	public ServerManager(final InetAddress ip, final int port, final int backlog, final int timeout, final ServerSocketFactory serverfactory, final Collection<InetAddress> onlineips, final ServerWorker worker, final short connectcount) {
		this.ip = ip;
		this.port = port;
		this.backlog = backlog;
		this.timeout = timeout;
		this.serverfactory = serverfactory;
		this.onlineips = onlineips;
		this.worker = worker;
		this.connectcount = connectcount;
	}
	
	public void start() {
		short connectcount = this.connectcount;
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
		
		serverwatcher.start();
	}
	
	public void restart() {
		serverwatcher.restart();
	}
	
	public void end() {
		serverwatcher.end();
		serverwatcher = null;
	}

}
