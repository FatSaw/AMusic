package me.bomb.amusic.resource;

import java.util.UUID;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.source.SoundSource;

public final class ResourceFactory implements Runnable {
	
	private final String id;
	private final UUID[] targets;
	private final Data datamanager;
	private final ResourceDispatcher dispatcher;
	private final SoundSource source;
	private final boolean update;
	private final StatusReport statusreport;
	
	public ResourceFactory(String id, UUID[] targets, Data datamanager, ResourceDispatcher dispatcher, SoundSource source, boolean update, StatusReport statusreport, boolean async) {
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
		DataEntry dataentry = datamanager.getPlaylist(this.id);
		if(update) {
			if(datamanager.lockwrite) {
				if(statusreport != null) statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
				return;
			}
			ResourcePacker resourcepacker = datamanager.createPacker(this.id, source);
			
			final boolean updated = datamanager.update(this.id, resourcepacker);
			if(resourcepacker == null) {
				if(statusreport == null) {
					return;
				}
				statusreport.onStatusResponse(updated ? EnumStatus.REMOVED : EnumStatus.NOTEXSIST);
				return;
			}
			dispatcher.resourcemanager.putCache(this.id, resourcepacker.resourcepack);
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
		if(dispatcher.dispatch(dataentry, this.targets)) {
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
