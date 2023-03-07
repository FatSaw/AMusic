package me.bomb.amusic;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

class ResourceServer extends Thread {
	private Set<InetAddress> downloaders = new HashSet<InetAddress>();
	private boolean end;
	private ServerSocket server;
	private BukkitTask task;
	int ophc = 0;
	public ResourceServer(AMusic plugin) {
		task = new BukkitRunnable() {
			@Override
			public void run() {
				int aopch = Bukkit.getOnlinePlayers().hashCode();
				if(ophc != aopch) {
					ophc = aopch;
					Set<InetAddress> adownloaders = new HashSet<InetAddress>();
					for(Player player : Bukkit.getOnlinePlayers()) {
						adownloaders.add(player.getAddress().getAddress());
					}
					downloaders = adownloaders;
				}	
			}
		}.runTaskTimer(plugin, 20L, 20L);
		this.end = false;
		start();
	}
	public void run() {
		while(true) {
			try {
				server = new ServerSocket(ConfigOptions.port);
	         } catch (Exception e) {
	            e.printStackTrace();
	            return;
	         }
			Socket connected = null;
	         while(!server.isClosed()) {
	            try {
	            	synchronized(downloaders) {
			               connected = server.accept();
			               if(downloaders.contains(connected.getInetAddress())) {
			                  new ResourceSender(connected);
			               }
	            	}
	            } catch (IOException e) {
	            }
	         }
	        if(end) break;
		}
	}
	@SuppressWarnings("deprecation")
	public void close() {
	      end = true;
	      task.cancel();
	      try {
	         server.close();
	      } catch (IOException e) {
	         e.printStackTrace();
	      }
	      stop();
	}
	
	private class ResourceSender extends Thread {
		private final Socket client;
		private ResourceSender(Socket client) {
			this.client = client;
			start();
		}
		public void run() {
		      try {
		  		BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		  		DataOutputStream  out = new DataOutputStream(this.client.getOutputStream());
		         StringTokenizer tokenizer = new StringTokenizer(in.readLine());
		         String httpMethod = tokenizer.nextToken();
		         String httpQueryString = tokenizer.nextToken();
		         UUID token = null;
		         if(httpQueryString.endsWith(".zip")) {
		        	 try {
		        		 token = UUID.fromString(httpQueryString.substring(1,httpQueryString.lastIndexOf(".zip")));
		        	 } catch (IllegalArgumentException e) {
		        		 client.close();
		        		 return;
		        	 }
		         }
		         byte[] ares = CachedResource.get(token);
		         if(ares.length!=0) {
			         try {
			            if(httpMethod.equals("GET") && !this.client.isClosed()) {
			            	out.writeBytes("HTTP/1.1 200 OK\r\n");
			            	out.writeBytes("Server: AMusic server");
			            	out.writeBytes("Content-Type: application/zip\r\n");
			            	out.writeBytes("Content-Length: " + Integer.toString(ares.length) + "\r\n");
			            	out.writeBytes("Connection: close\r\n");
			            	out.writeBytes("\r\n");
			            	out.write(ares);
			            }
			         } catch (Exception e) {
			        	e.printStackTrace();
			            return;
			         }
		         }
		         client.close();
		         
		      } catch (Exception e) {
		         e.printStackTrace();
		      }

		   }
	}

}
