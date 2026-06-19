package me.bomb.amusic;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePack.Builder;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.pack.GeyserResourcePack;
import org.geysermc.geyser.pack.GeyserResourcePackManifest;
import org.geysermc.geyser.pack.GeyserResourcePackManifest.Header;
import org.geysermc.geyser.pack.GeyserResourcePackManifest.Module;
import org.geysermc.geyser.pack.GeyserResourcePackManifest.Version;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.util.ReadOnlyByteArrayChannel;

public final class GeyserHook implements EventRegistrar {

	private final Data datamanager;
	private final GeyserApi api;

	public GeyserHook(Data datamanager) throws NoClassDefFoundError {
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
			ResourcePack pack = new BufPackCodec(entry).create();
			event.register(pack, PriorityOption.NORMAL);
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
			Version version = new Version(1, 0, 0);
			Header header = new Header(entry.bhea, version, "AMusic resourcepack", "DESCRIPTION", new Version(1, 14, 0));
			Module module = new Module(entry.bres, version, "resources", "");
			HashSet<Module> modules = new HashSet<>(1);
			modules.add(module);
			GeyserResourcePackManifest manifest = new GeyserResourcePackManifest(2, header, modules, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
			return new GeyserResourcePack(this, manifest, entry.name);
	    }

		@Override
		protected Builder createBuilder() {
			return null;
		}
		
	}
}
