package me.bomb.amusic.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

public final class SimpleServerSocketFactory extends ServerSocketFactory {

	@Override
	public ServerSocket createServerSocket() throws IOException {
		return new ServerSocket();
	}
	
	@Override
	public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
		return new ServerSocket(port, backlog, ifAddress);
	}
	
	@Override
	public ServerSocket createServerSocket(int port, int backlog) throws IOException {
		return new ServerSocket(port, backlog);
	}
	
	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		return new ServerSocket(port);
	}

}
