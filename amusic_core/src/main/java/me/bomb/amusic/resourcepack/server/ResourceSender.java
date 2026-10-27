package me.bomb.amusic.resourcepack.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.bomb.amusic.config.ExecutorConfiguration;
import me.bomb.amusic.http.ServerWorker;
import me.bomb.amusic.resourcepack.DataEntry;
import me.bomb.amusic.util.AMusicLogger;

final class ResourceSender implements ServerWorker {
	
	private static final ByteBuffer responsepart0 = ByteBuffer.wrap("HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nConnection: close\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII)), responsepart1 = ByteBuffer.wrap("\r\n\r\n".getBytes(StandardCharsets.US_ASCII)), requestinvalid = ByteBuffer.wrap("HTTP/1.1 400 Bad Request\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer(), tokeninvalid = ByteBuffer.wrap("HTTP/1.1 401 Unauthorized\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer(), waitacceptiontimeout = ByteBuffer.wrap("HTTP/1.1 408 Request Timeout\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer();

	private final ResourceManager resourcemanager;
	private final ExecutorConfiguration senderexecutorcfg;
	private final int waitacceptioncount, waitacceptionwait, schedulerthreads;
	private ExecutorService senderexecutor = null;
	private ScheduledExecutorService scheduler = null;

	protected ResourceSender(ResourceManager resourcemanager, ExecutorConfiguration senderexecutorcfg, int waitacceptioncount, int waitacceptionwait, int schedulerthreads) {
		this.resourcemanager = resourcemanager;
		this.senderexecutorcfg = senderexecutorcfg;
		this.waitacceptioncount = waitacceptioncount;
		this.waitacceptionwait = waitacceptionwait;
		this.schedulerthreads = schedulerthreads;
	}

	@Override
	public void processConnection(final SocketChannel connected) throws IOException {
		Runnable r = new RequestHandler(this.senderexecutor, connected, resourcemanager, scheduler, this.waitacceptioncount, this.waitacceptionwait);
		this.senderexecutor.execute(r);
	}

	@Override
	public void start() {
		this.senderexecutor = this.senderexecutorcfg.createExecutor();
		if (this.schedulerthreads < 0) {
			return;
		}
		this.scheduler = new ScheduledThreadPoolExecutor(this.schedulerthreads);
	}

	@Override
	public void end() {
		this.senderexecutor.shutdown();
		this.senderexecutor = null;
		if (this.scheduler == null) {
			return;
		}
		this.scheduler.shutdown();
		this.scheduler = null;
	}

	private static void write(SocketChannel connected, ByteBuffer response) throws IOException {
		ByteBuffer buffer = response.duplicate();
		while (buffer.hasRemaining()) {
			connected.write(buffer);
		}
	}

	private final class RequestHandler implements Runnable {

		private final Executor senderexecutor;
		private final SocketChannel connected;
		private final ResourceManager resourcemanager;
		private final ScheduledExecutorService scheduler;
		private final int waitacceptioncount, waitacceptionwait;

		private RequestHandler(Executor senderexecutor, SocketChannel connected, ResourceManager resourcemanager, ScheduledExecutorService scheduler, int waitacceptioncount, int waitacceptionwait) {
			this.senderexecutor = senderexecutor;
			this.connected = connected;
			this.resourcemanager = resourcemanager;
			this.scheduler = scheduler;
			this.waitacceptioncount = waitacceptioncount;
			this.waitacceptionwait = waitacceptionwait;
		}

		private void close() {
			try {
				this.connected.close();
			} catch (IOException e) {
			}
		}

		@Override
		public void run() {
			byte[] buf = new byte[0x400];
			int off = 0, rem = buf.length, n;
			byte readattempts = 16;
			try {
				InputStream in = this.connected.socket().getInputStream();
				while (off < buf.length && (n = in.read(buf, off, rem)) != -1 && --readattempts > -1) {
					off += n;
					rem -= n;
					if(off < 4 || '\n' == buf[off - 1] && '\r' == buf[off - 2] && '\n' == buf[off - 3] && '\r' == buf[off - 4]) {
						break;
					}
				}
				if(readattempts == -1 && off < 54) {
					AMusicLogger.warn("Socket too many read count!");
					this.close();
					return;
				}
			} catch (SocketTimeoutException e) {
				AMusicLogger.warn("Socket read timeout: ".concat(e.getMessage()));
				this.close();
				return;
			} catch (IOException e) {
				this.close();
				return;
			}
			if(off < 54 || !(buf[0] == 'G' && buf[1] == 'E' && buf[2] == 'T' && buf[3] == ' ' && buf[4] == '/' && buf[41] == '.' && buf[42] == 'z' && buf[43] == 'i' && buf[44] == 'p' && buf[45] == ' ' && buf[46] == 'H' && buf[47] == 'T' && buf[48] == 'T' && buf[49] == 'P' && buf[50] == '/' && buf[51] == '1' && buf[52] == '.' && buf[53] == '1')) {
				try {
					write(this.connected, requestinvalid);
				} catch (IOException e) {
				}
				this.close();
				return;
			}
			final UUID token;
			try {
				token = UUID.fromString(new String(buf, 5, 36, StandardCharsets.US_ASCII));
			} catch (IndexOutOfBoundsException | IllegalArgumentException e1) {
				try {
					write(this.connected, requestinvalid);
					this.connected.close();
				} catch (IOException e2) {
				}
				this.close();
				return;
			}

			ResponseSender responsesender = new ResponseSender(this.connected, this.resourcemanager, token);

			if (this.scheduler != null && this.resourcemanager.waitAcception(token)) {
				WaitAcception sendresponse = new WaitAcception(this.scheduler, this.senderexecutor, this.resourcemanager, responsesender, this.waitacceptioncount, this.waitacceptionwait);
				this.scheduler.schedule(sendresponse, waitacceptionwait, TimeUnit.MILLISECONDS);
				return;
			}

			responsesender.run();
		}
	}

	private final class WaitAcception implements Runnable {
		
		private final ScheduledExecutorService scheduler;
		private final Executor senderexecutor;
		private final ResourceManager resourcemanager;
		private final ResponseSender responsesender;
		private final int waitacceptionwait;
		private int i;

		private WaitAcception(ScheduledExecutorService scheduler, Executor senderexecutor, ResourceManager resourcemanager, ResponseSender responsesender, int waitacceptioncount, int waitacceptionwait) {
			this.scheduler = scheduler;
			this.senderexecutor = senderexecutor;
			this.resourcemanager = resourcemanager;
			this.responsesender = responsesender;
			this.waitacceptionwait = waitacceptionwait;
			i = waitacceptioncount;
		}

		@Override
		public void run() {
			if (this.resourcemanager.waitAcception(this.responsesender.token)) {
				if(0 != i && !this.scheduler.isShutdown()) {
					--i;
					this.scheduler.schedule(this, this.waitacceptionwait, TimeUnit.MILLISECONDS);
					return;
				}
				try {
					write(this.responsesender.connected, waitacceptiontimeout);
				} catch (IOException e) {
				}
				try {
					this.responsesender.connected.close();
				} catch (IOException e) {
				}
				return;
			}
			this.senderexecutor.execute(this.responsesender);
		}
	}
	
	private final class ResponseSender implements Runnable {
		
		private final SocketChannel connected;
		private final ResourceManager resourcemanager;
		private final UUID token;
		
		private ResponseSender(SocketChannel connected, ResourceManager resourcemanager, UUID token) {
			this.connected = connected;
			this.resourcemanager = resourcemanager;
			this.token = token;
		}
		
		private void close() {
			try {
				this.connected.close();
			} catch (IOException e) {
			}
		}
		
		@Override
		public void run() {
			DataEntry entry;
			if ((entry = this.resourcemanager.get(this.token)) == null) {
				try {
					write(this.connected, tokeninvalid);
				} catch (IOException e) {
				}
				this.close();
				return;
			}
			int length = entry.getLength();
			try {
				ByteBuffer[] p1 = new ByteBuffer[3];
				p1[0] = responsepart0.duplicate();
				p1[1] = ByteBuffer.wrap(Integer.toString(length).getBytes(StandardCharsets.US_ASCII));
				p1[2] = responsepart1.duplicate();
				this.connected.write(p1);
				entry.sendTo(this.connected);
			} catch (IOException e) {
			} finally {
				this.close();
			}
		}
	}
}
