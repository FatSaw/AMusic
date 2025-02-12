package me.bomb.amusic.resource;

import java.io.File;
import java.util.UUID;

import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.packedinfo.DataStorage;
import me.bomb.amusic.source.SoundSource;

import static me.bomb.amusic.util.NameFilter.filterName;
import static me.bomb.amusic.util.Base64Utils.toBase64Url;

public final class ResourceFactory implements Runnable {
	
	private final String id;
	private final UUID[] targets;
	private final DataStorage datamanager;
	private final ResourceDispatcher dispatcher;
	private final SoundSource<?> source;
	private final boolean update;
	private final StatusReport statusreport;
	
	public ResourceFactory(String id, UUID[] targets, DataStorage datamanager, ResourceDispatcher dispatcher, SoundSource<?> source, boolean update, StatusReport statusreport, boolean async) {
		this.id = id;
		this.targets = targets;
		this.datamanager = datamanager;
		this.dispatcher = dispatcher;
		this.source = source;
		this.update = update;
		this.statusreport = statusreport;
		if(async) {
			new Thread(this).start();
		} else {
			run();
		}
	}

	@Override
	public void run() {
		String id = toBase64Url(this.id);
		File resourcefile = new File(datamanager.datadirectory, id.concat(".zip"));
		
		DataEntry dataentry = datamanager.getPlaylist(this.id);
		if(update) {
			final String name = dataentry == null ? filterName(this.id) : dataentry.name;
			Object f = source.getSource();
			File sourcearchive = f instanceof File ? new File((File) f, name.concat(".zip")) : null;
			ResourcePacker resourcepacker = null;
			if(!datamanager.update(this.id, source.exists(this.id) ? resourcepacker = new ResourcePacker(source, this.id, resourcefile, sourcearchive, dispatcher.resourcemanager) : null)) {
				if(statusreport != null) statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
				return;
			}
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
			if(targets == null) {
				if(statusreport == null) {
					return;
				}
				statusreport.onStatusResponse(EnumStatus.PACKED);
				return;
			}
			dispatcher.dispatch(this.id, this.targets, resourcepacker.sounds, resourcepacker.resourcepack, resourcepacker.sha1);
			if(statusreport == null) {
				return;
			}
			statusreport.onStatusResponse(EnumStatus.DISPATCHED);
			return;
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
		if(dispatcher.dispatch(this.id, this.targets, dataentry.sounds, resourcefile, dataentry.sha1)) {
			if(statusreport == null) {
				return;
			}
			statusreport.onStatusResponse(EnumStatus.DISPATCHED);
			return;
		}
		if(statusreport == null) {
			return;
		}
		statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
	}
}
