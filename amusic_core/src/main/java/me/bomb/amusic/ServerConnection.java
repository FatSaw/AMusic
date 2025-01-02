package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerConnection extends Thread {
	
	private final ServerAMusic serveramusic;
	private final InetAddress ip, remoteip;
	private final int port, backlog;
	private boolean run;
	private ServerSocket server;
	
	public ServerConnection(ServerAMusic serveramusic, InetAddress ip, InetAddress remoteip, int port, int backlog) {
		this.serveramusic = serveramusic;
		this.ip = ip;
		this.remoteip = remoteip;
		this.port = port;
		this.backlog = backlog;
	}
	
	@Override
	public void start() {
		this.run = true;
		super.start();
	}
	
	@Override
	public void run() {
		while (run) {
			try {
				server = new ServerSocket();
				server.bind(new InetSocketAddress(ip, port), backlog);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				Socket connected = null;
				try {
					connected = server.accept();
					if (this.remoteip == null || this.remoteip.equals(connected.getInetAddress())) {
						processConnection(connected);
					}
				} catch (IOException e) {
				} finally {
					if(connected != null) {
						try {
							connected.close();
						} catch (IOException e) {
						}
					}
				}
			}
		}
	}
	
	public void end() {
		run = false;
		if(server==null) return;
		try {
			server.close();
		} catch (IOException e) {
		}
	}
	
	private final void processConnection(Socket connected) throws IOException {
		InputStream is = connected.getInputStream();
		byte[] buf = new byte[9];
		if(is.read(buf) != 9 || buf[0] != 'a' || buf[1] != 'm' || buf[2] != 'r' || buf[3] != 'a' || buf[4] != '0' || buf[7] != '0') {
			return;
		}
		final byte packetid = buf[8];
		if(packetid != 0x02 || packetid != 0x12) {
			buf = new byte[4];
			int length = (0xFF & buf[12]) << 24 | (0xFF & buf[11]) << 16 | (0xFF & buf[10]) << 8 | 0xFF & buf[9];
			buf = new byte[length];
			if(packetid < 0 || packetid > 0x13 || length != is.read(buf)) {
				return;
			}
		}
		connected.shutdownInput();
		OutputStream os = connected.getOutputStream();
		int size;
		switch(packetid) {
		case 0x01:
			buf = serveramusic.getPlayersLoadedBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x02:
			buf = serveramusic.getPlaylistsBytes();
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x03:
			buf = serveramusic.getPlaylistSoundnamesPlaylistnameBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x04:
			buf = serveramusic.getPlaylistSoundnamesPlayeruuidBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x05:
			buf = serveramusic.getPlaylistSoundlengthsPlaylistnameBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x06:
			buf = serveramusic.getPlaylistSoundlengthsPlayeruuidBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x07:
			serveramusic.setRepeatModeBytes(buf);
		break;
		case 0x08:
			buf = serveramusic.getPlayingSoundNameBytes(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x09:
			os.write(serveramusic.getPlayingSoundSizeBytes(buf));
		break;
		case 0x0A:
			os.write(serveramusic.getPlayingSoundRemainBytes(buf));
		break;
		case 0x0B:
			os.write(serveramusic.loadPackBytes(buf));
		break;
		case 0x0C:
			buf = serveramusic.getPackName(buf);
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x0D:
			serveramusic.stopSoundBytes(buf);
		break;
		case 0x0E:
			serveramusic.stopSoundUntrackableBytes(buf);
		break;
		case 0x0F:
			serveramusic.playSoundBytes(buf);
		break;
		case 0x10:
			serveramusic.playSoundUntrackableBytes(buf);
		break;
		case 0x11:
			os.write(serveramusic.openUploadSessionBytes(buf));
		break;
		case 0x12:
			buf = serveramusic.getUploadSessionsBytes();
			size = buf.length;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			size>>=8;
			os.write((byte)size);
			os.write(buf);
		break;
		case 0x13:
			os.write(serveramusic.closeUploadSessionBytes(buf));
		break;
		}
	}

}
