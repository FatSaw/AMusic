package me.bomb.amusic;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import org.geysermc.geyser.api.pack.ResourcePack.Builder;
import org.geysermc.geyser.api.pack.option.UrlFallbackOption;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.util.ReadOnlyByteArrayChannel;

public final class GeyserHook implements EventRegistrar {

	private final Data datamanager;
	private final GeyserApi api;

	public GeyserHook(Data datamanager) throws ClassNotFoundException {
		this.datamanager = datamanager;
		this.api = GeyserApi.api();
		api.eventBus().register(this, this);
	}

	public boolean isBedrockPlayer(UUID playeruuid) {
		return playeruuid != null && this.api.isBedrockPlayer(playeruuid);
	}

	@Subscribe
	public void onSessionLoadResourcePacksEvent(SessionLoadResourcePacksEvent event) {
		String[] playlists = this.datamanager.getPlaylists();
		int i = playlists.length;
		while(--i > -1) {
			DataEntry entry = this.datamanager.getPlaylist(playlists[i]);
			if(entry == null) {
				continue;
			}
			event.register(new BufPackCodec(entry).create(), UrlFallbackOption.TRUE);
		}
	}
	
	public final static class BufResourcePack implements ResourcePack {
		private final PackCodec codec;
		private final ResourcePackManifest manifest;
		private final String contentKey;
		
		protected BufResourcePack(PackCodec codec, ResourcePackManifest manifest, String contentKey) {
			this.codec = codec;
			this.manifest = manifest;
			this.contentKey = contentKey;
		}

		@Override
		public PackCodec codec() {
			return this.codec;
		}

		@Override
		public ResourcePackManifest manifest() {
			return this.manifest;
		}

		@Override
		public String contentKey() {
			return this.contentKey;
		}
	}
	public final static class BufPackCodec extends PackCodec {

		private final DataEntry entry;

		protected BufPackCodec(DataEntry entry) {
			this.entry = entry;
		}

		@Override
		public byte[] sha256() {
			return this.entry.sha256;
		}

		@Override
		public long size() {
			return this.entry.size;
		}

		@Override
		public SeekableByteChannel serialize() throws IOException {
			return new ReadOnlyByteArrayChannel(this.entry.getPack());
		}
		
		@Override
	    protected ResourcePack create() {
	        return createBuilder().build();
	    }

		@Override
		protected Builder createBuilder() {
			return null;
		}
	}
}
