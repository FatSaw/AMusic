package me.bomb.amusic.sponge;

import static me.bomb.amusic.util.HexUtils.fromBytesToHex;

import java.net.URISyntaxException;
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
				rp = new SpongeURIResourcePack(url, fromBytesToHex(sha1));
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
	
}
