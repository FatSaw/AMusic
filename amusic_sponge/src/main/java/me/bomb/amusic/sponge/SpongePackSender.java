package me.bomb.amusic.sponge;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.common.resourcepack.SpongeURIResourcePack;

import me.bomb.amusic.PackSender;

public final class SpongePackSender implements PackSender {
	
	private final Server server;
	
	protected SpongePackSender(Server server) {
		this.server = server;
	}

	@Override
	public void send(UUID uuid, String url, byte[] sha1) {
		if(uuid == null) {
			return;
		}
		Optional<Player> oplayer = server.getPlayer(uuid);
		if(oplayer.isPresent()) {
			Player player = oplayer.get();
			SpongeURIResourcePack rp;
			try {
				rp = new SpongeURIResourcePack(url, bytesToHex(sha1));
			} catch (URISyntaxException e) {
				return;
			}
			player.sendResourcePack(rp);
			/*player.sendResourcePack(new ResourcePack() {
				
				final String name = url.substring(url.lastIndexOf("/") + 1).replaceAll("\\W", "");
				
				@Override
				public URI getUri() {
					return URI.create(url);
				}
				
				@Override
				public String getName() {
					return name;
				}
				
				@Override
				public String getId() {
					return uuid.toString();
				}
				
				@Override
				public Optional<String> getHash() {
					return Optional.of(bytesToHex(sha1));
				}
			});*/
		}
	}
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
	private static String bytesToHex(byte[] bytes) {
		int i = bytes.length, j = i << 1;
	    byte[] hexChars = new byte[j];
	    while(--i > -1) {
	    	int v = bytes[i] & 0xFF;
	        hexChars[--j] = HEX_ARRAY[v & 0x0F];
	    	hexChars[--j] = HEX_ARRAY[v >>> 4];
	    }
	    return new String(hexChars, StandardCharsets.US_ASCII);
	}
	
}
