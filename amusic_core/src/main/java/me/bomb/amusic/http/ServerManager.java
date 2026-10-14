package me.bomb.amusic.http;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collection;

public final class ServerManager {
	
	private final SocketAddress address;
	private final int backlog;
	private final int timeout;
	private final Collection<InetAddress> onlineips;
	private final ServerWorker worker;
	private final short connectcount;
	
	private ServerWatcher serverwatcher;
	
	public ServerManager(final SocketAddress address, final int backlog, final int timeout, final Collection<InetAddress> onlineips, final ServerWorker worker, final short connectcount) {
		this.address = address;
		this.backlog = backlog;
		this.timeout = timeout;
		this.onlineips = onlineips;
		this.worker = worker;
		this.connectcount = connectcount;
	}
	
	public void start() {
		short connectcount = this.connectcount;
		ServerConnect[] connects = new ServerConnect[connectcount];
		this.serverwatcher = new ServerWatcher(address, backlog, timeout, connects);
		if(onlineips == null) {
			while(--connectcount > -1) {
				connects[connectcount] = new ServerConnect(serverwatcher, worker);
			}
		} else {
			while(--connectcount > -1) {
				connects[connectcount] = new ConnectedServerConnect(serverwatcher, worker, onlineips);
			}
		}
		this.worker.start();
		this.serverwatcher.start();
	}
	
	public void restart() {
		this.serverwatcher.restart();
	}
	
	public void end() {
		this.worker.end();
		this.serverwatcher.end();
		this.serverwatcher = null;
	}

}
