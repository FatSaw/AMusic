package me.bomb.amusic.resourceserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.bomb.amusic.http.ServerWorker;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.util.AMusicLogger;

final class ResourceSender implements ServerWorker {
	
	private static final byte[] responsepart0 = "HTTP/1.1 200 OK\r\nServer: AMusic server\r\nContent-Type: application/zip\r\nConnection: close\r\nContent-Length: ".getBytes(), responsepart1 = "\r\n\r\n".getBytes(), requestinvalid = "HTTP/1.1 400 Bad Request\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII), tokeninvalid = "HTTP/1.1 401 Unauthorized\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII), packreadfail = "HTTP/1.1 500 Internal Server Error\r\nServer: AMusic server\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
	
	private final ResourceManager resourcemanager;
	private final Executor senderexecutor;
	private final int waitacceptioncount, waitacceptionwait, schedulerthreads;
	private ScheduledExecutorService scheduler = null;

	protected ResourceSender(ResourceManager resourcemanager, Executor senderexecutor, int waitacceptioncount, int waitacceptionwait, int schedulerthreads) {
		this.resourcemanager = resourcemanager;
		this.senderexecutor = senderexecutor;
		this.waitacceptioncount = waitacceptioncount;
		this.waitacceptionwait = waitacceptionwait;
		this.schedulerthreads = schedulerthreads;
	}

	@Override
	public void processConnection(final Socket connected) throws IOException {
		Runnable r = new RequestHandler(this.senderexecutor, connected, resourcemanager, scheduler);
		this.senderexecutor.execute(r);
	}
	
	@Override
	public void start() {
		if(this.schedulerthreads < 0) {
			return;
		}
		this.scheduler = new ScheduledThreadPoolExecutor(this.schedulerthreads);
	}
	
	@Override
	public void end() {
		if(this.scheduler == null) {
			return;
		}
		this.scheduler.shutdown();
		this.scheduler = null;
	}
	
	private final class RequestHandler implements Runnable {
		
		private final Executor senderexecutor;
		private final Socket connected;
		private final ResourceManager resourcemanager;
		private final ScheduledExecutorService scheduler;
		
		private RequestHandler(Executor senderexecutor, Socket connected, ResourceManager resourcemanager, ScheduledExecutorService scheduler) {
			this.senderexecutor = senderexecutor;
			this.connected = connected;
			this.resourcemanager = resourcemanager;
			this.scheduler = scheduler;
		}
		
		private void close() {
			try {
				this.connected.close();
            } catch (IOException e) {
            }
		}

		@Override
		public void run() {
			byte[] buf = new byte[0x200];
            int off = 0, rem = buf.length, n;
            try {
            	InputStream in = this.connected.getInputStream();
            	while (off < buf.length && (n = in.read(buf, off, rem)) != -1) {
            		off += n;
            		rem -= off;
            		if(off < 4 || '\n' == buf[off - 1] && '\r' == buf[off - 2] && '\n' == buf[off - 3] && '\r' == buf[off - 4]) {
        				break;
        			}
            	}
            	this.connected.shutdownInput(); //HTTPS NOT SUPPORT THIS
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
                	OutputStream out = this.connected.getOutputStream();
    				out.write(requestinvalid);
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
            		OutputStream out = this.connected.getOutputStream();
    				out.write(requestinvalid);
    				this.connected.close();
            	} catch (IOException e2) {
                }
            	this.close();
                return;
            }
            ResponseSender responsesender = new ResponseSender(this.connected, this.resourcemanager, token);
            if(this.scheduler != null && this.resourcemanager.waitAcception(token)) {
            	WaitAcception sendresponse = new WaitAcception(this.scheduler, this.senderexecutor, this.resourcemanager, responsesender);
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
		private int i = waitacceptioncount;

		private WaitAcception(ScheduledExecutorService scheduler, Executor senderexecutor, ResourceManager resourcemanager, ResponseSender responsesender) {
			this.scheduler = scheduler;
			this.senderexecutor = senderexecutor;
			this.resourcemanager = resourcemanager;
			this.responsesender = responsesender;
		}

		@Override
		public void run() {
			if (this.resourcemanager.waitAcception(this.responsesender.token)) {
				if(0 != i && !this.scheduler.isShutdown()) {
					--i;
					this.scheduler.schedule(this, waitacceptionwait, TimeUnit.MILLISECONDS);
				}
				return;
			}
			this.senderexecutor.execute(this.responsesender);
		}
	}
	
	private final class ResponseSender implements Runnable {
		
		private final Socket connected;
		private final ResourceManager resourcemanager;
		private final UUID token;
		
		private ResponseSender(Socket connected, ResourceManager resourcemanager, UUID token) {
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
					OutputStream out = this.connected.getOutputStream();
					out.write(tokeninvalid);
	            } catch (IOException e) {
	            }
				this.close();
				return;
			}
			byte[] buf;
			if ((buf = entry.getPack()) == null) {
				try {
					OutputStream out = this.connected.getOutputStream();
					out.write(packreadfail);
	            } catch (IOException e) {
	            }
				this.close();
				return;
			}
			
			try(OutputStream out = connected.getOutputStream()) {
				out.write(responsepart0);
				out.write(Integer.toString(buf.length).getBytes());
				out.write(responsepart1);
				out.write(buf);
			} catch(IOException e) {
			} finally {
				this.close();
			}
		}
	}
}
