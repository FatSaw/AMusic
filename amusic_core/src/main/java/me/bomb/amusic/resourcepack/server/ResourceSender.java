package me.bomb.amusic.resourcepack.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.bomb.amusic.http.ServerWorker;
import me.bomb.amusic.resourcepack.DataEntry;
import me.bomb.amusic.util.AMusicLogger;

final class ResourceSender implements ServerWorker {
	
	private static final ByteBuffer responsepart0 = ByteBuffer.wrap("HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nConnection: close\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII)), responsepart1 = ByteBuffer.wrap("\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
	//private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nConnection: close\r\nContent-Length: ".getBytes(StandardCharsets.US_ASCII), responsepart1 = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), requestinvalid = "HTTP/1.1 400 Bad Request\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII), tokeninvalid = "HTTP/1.1 401 Unauthorized\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII), waitacceptiontimeout = "HTTP/1.1 408 Request Timeout\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	private static final ByteBuffer requestinvalid = ByteBuffer.wrap(
			"HTTP/1.1 400 Bad Request\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
					.getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer(),
			tokeninvalid = ByteBuffer.wrap(
					"HTTP/1.1 401 Unauthorized\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
							.getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer(),
			waitacceptiontimeout = ByteBuffer.wrap(
					"HTTP/1.1 408 Request Timeout\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
							.getBytes(StandardCharsets.US_ASCII)).asReadOnlyBuffer();

	private final ResourceManager resourcemanager;
	private final Executor senderexecutor;
	private final int waitacceptioncount, waitacceptionwait, schedulerthreads;
	private ScheduledExecutorService scheduler = null;

	protected ResourceSender(ResourceManager resourcemanager, Executor senderexecutor,
			int waitacceptioncount, int waitacceptionwait, int schedulerthreads) {
		this.resourcemanager = resourcemanager;
		this.senderexecutor = senderexecutor;
		this.waitacceptioncount = waitacceptioncount;
		this.waitacceptionwait = waitacceptionwait;
		this.schedulerthreads = schedulerthreads;
	}

	@Override
	public void processConnection(final SocketChannel connected) throws IOException {
		Runnable r = new RequestHandler(this.senderexecutor, connected, resourcemanager,
				scheduler, this.waitacceptioncount, this.waitacceptionwait);
		this.senderexecutor.execute(r);
	}

	@Override
	public void start() {
		if (this.schedulerthreads < 0) {
			return;
		}
		this.scheduler = new ScheduledThreadPoolExecutor(this.schedulerthreads);
	}

	@Override
	public void end() {
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

		private RequestHandler(Executor senderexecutor, SocketChannel connected,
				ResourceManager resourcemanager, ScheduledExecutorService scheduler,
				int waitacceptioncount, int waitacceptionwait) {
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
			ByteBuffer buf = ByteBuffer.allocate(0x400);
			int n;

			try {
				while (buf.hasRemaining() && (n = this.connected.read(buf)) != -1) {
					int off = buf.position();

					if (off < 4 || buf.get(off - 1) == '\n'
							&& buf.get(off - 2) == '\r'
							&& buf.get(off - 3) == '\n'
							&& buf.get(off - 4) == '\r') {
						break;
					}
				}
			} catch (IOException e) {
				if (e instanceof SocketTimeoutException) {
					AMusicLogger.warn("Socket read timeout: ".concat(e.getMessage()));
				}
				this.close();
				return;
			}

			int off = buf.position();

			if (off < 54 || !(buf.get(0) == 'G'
					&& buf.get(1) == 'E'
					&& buf.get(2) == 'T'
					&& buf.get(3) == ' '
					&& buf.get(4) == '/'
					&& buf.get(41) == '.'
					&& buf.get(42) == 'z'
					&& buf.get(43) == 'i'
					&& buf.get(44) == 'p'
					&& buf.get(45) == ' '
					&& buf.get(46) == 'H'
					&& buf.get(47) == 'T'
					&& buf.get(48) == 'T'
					&& buf.get(49) == 'P'
					&& buf.get(50) == '/'
					&& buf.get(51) == '1'
					&& buf.get(52) == '.'
					&& buf.get(53) == '1')) {
				try {
					write(this.connected, requestinvalid);
				} catch (IOException e) {
				}
				this.close();
				return;
			}

			final UUID token;
			try {
				ByteBuffer tokenbuffer = buf.duplicate();
				tokenbuffer.position(5);
				tokenbuffer.limit(41);
				byte[] tokenbytes = new byte[36];
				tokenbuffer.get(tokenbytes);
				token = UUID.fromString(new String(tokenbytes, StandardCharsets.US_ASCII));
			} catch (IndexOutOfBoundsException | IllegalArgumentException e1) {
				try {
					write(this.connected, requestinvalid);
				} catch (IOException e2) {
				}
				this.close();
				return;
			}

			ResponseSender responsesender = new ResponseSender(this.connected, this.resourcemanager, token);

			if (this.scheduler != null && this.resourcemanager.waitAcception(token)) {
				WaitAcception sendresponse = new WaitAcception(this.scheduler,
						this.senderexecutor, this.resourcemanager, responsesender,
						this.waitacceptioncount, this.waitacceptionwait);
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

		private WaitAcception(ScheduledExecutorService scheduler, Executor senderexecutor,
				ResourceManager resourcemanager, ResponseSender responsesender,
				int waitacceptioncount, int waitacceptionwait) {
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
				if (0 != i && !this.scheduler.isShutdown()) {
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
					this.connected.write(tokeninvalid);
				} catch (IOException e) {
				}
				this.close();
				return;
			}
			int length = entry.getLength();
			try {
				ByteBuffer[] p1 = new ByteBuffer[3];
				p1[0] = responsepart0;
				p1[1] = ByteBuffer.wrap(Integer.toString(length).getBytes(StandardCharsets.US_ASCII));
				p1[2] = responsepart1;
				this.connected.write(p1);
				entry.sendTo(this.connected);
			} catch (IOException e) {
			} finally {
				this.close();
			}
		}
	}
}
