package me.bomb.amusic.resource;

import java.util.UUID;

import me.bomb.amusic.packedinfo.Data;
import me.bomb.amusic.packedinfo.DataEntry;
import me.bomb.amusic.resourceserver.ResourceManager;

public final class ResourceFactory implements Runnable {
	
	private final String id;
	private final UUID[] targets;
	private final Data datamanager;
	private final ResourceManager resourcemanager;
	private final boolean update;
	private final StatusReport statusreport;
	
	public ResourceFactory(String id, UUID[] targets, Data datamanager, ResourceManager resourcemanager, boolean update, StatusReport statusreport) {
		this.id = id;
		this.targets = targets;
		this.datamanager = datamanager;
		this.resourcemanager = resourcemanager;
		this.update = update;
		this.statusreport = statusreport;
	}

	@Override
	public void run() {
		if(update) {
			if(datamanager.lockwrite) {
				if(statusreport != null) statusreport.onStatusResponse(EnumStatus.UNAVILABLE);
				return;
			}
			ResourcePacker resourcepacker = datamanager.createPacker(this.id);
			
			final boolean updated = datamanager.update(this.id, resourcepacker);
			if(resourcepacker == null) {
				if(statusreport == null) {
					return;
				}
				statusreport.onStatusResponse(updated ? EnumStatus.REMOVED : EnumStatus.NOTEXSIST);
				return;
			}
			if(targets == null) {
				if(statusreport == null) {
					return;
				}
				statusreport.onStatusResponse(EnumStatus.PACKED);
				return;
			}
			DataEntry dataentry = datamanager.getPlaylist(this.id);
			resourcemanager.dispatch(dataentry, this.targets);
			if(statusreport == null) {
				return;
			}
			statusreport.onStatusResponse(EnumStatus.DISPATCHED);
			return;
		}
		DataEntry dataentry = datamanager.getPlaylist(this.id);
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
		if(resourcemanager.dispatch(dataentry, this.targets)) {
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
