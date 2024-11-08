package me.bomb.amusic.resource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;

import me.bomb.amusic.dispatcher.ResourceDispatcher;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.packedinfo.DataManager;
import me.bomb.amusic.source.SoundSource;

public final class ResourceFactory extends Thread {
	
	private final String id;
	private final UUID[] targets;
	private final DataManager datamanager;
	private final ResourceDispatcher dispatcher;
	private final SoundSource<?> source;
	private final boolean update;
	private final Encoder base64encoder;
	private final StatusReport statusreport;
	
	public ResourceFactory(String id, UUID[] targets, DataManager datamanager, ResourceDispatcher dispatcher, SoundSource<?> source, boolean update, StatusReport statusreport) {
		this.id = id;
		this.targets = targets;
		this.datamanager = datamanager;
		this.dispatcher = dispatcher;
		this.source = source;
		this.update = update;
		this.statusreport = statusreport;
		this.base64encoder = Base64.getUrlEncoder();
		start();
	}
	
	private String toBase64(String name) {
		return new String(base64encoder.encode(name.getBytes(StandardCharsets.UTF_8)), StandardCharsets.US_ASCII);
	}

	@Override
	public void run() {
		String id = toBase64(this.id);
		File resourcefile = new File(datamanager.datadirectory, id.concat(".zip"));
		
		DataEntry dataentry = datamanager.getPlaylist(this.id);
		if(update) {
			final String name = dataentry == null ? DataManager.filterName(this.id) : dataentry.name;
			Object f = source.getSource();
			File sourcearchive = f instanceof File ? new File((File) f, name.concat(".zip")) : null;
			if(datamanager.update(this.id, source.exists(this.id) ? new ResourcePacker(source, this.id, resourcefile, sourcearchive, dispatcher.resourcemanager) : null)) {
				dataentry = datamanager.getPlaylist(this.id);
				if(dataentry == null) {
					if(resourcefile.exists()) {
						resourcefile.delete();
						if(statusreport == null) {
							return;
						}
						statusreport.onStatusResponse(EnumStatus.REMOVED);
						return;
					}
					if(statusreport == null) {
						return;
					}
					statusreport.onStatusResponse(EnumStatus.NOTEXSIST);
					return;
				}
			} else if(statusreport != null) {
				statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
				return;
			}
		}
		if(dataentry == null) {
			if(statusreport == null) {
				return;
			}
			statusreport.onStatusResponse(EnumStatus.NOTEXSIST);
			return;
		}
		if(targets == null) {
			if(statusreport == null) {
				return;
			}
			statusreport.onStatusResponse(EnumStatus.PACKED);
			return;
		}
		
		dispatcher.dispatch(this.id, this.targets, resourcefile, dataentry.sha1, dataentry.sounds);
		if(statusreport == null) {
			return;
		}
		statusreport.onStatusResponse(EnumStatus.DISPATCHED);
	}
}
