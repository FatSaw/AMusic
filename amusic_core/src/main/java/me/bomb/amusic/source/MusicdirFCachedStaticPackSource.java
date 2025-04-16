package me.bomb.amusic.source;

public final class MusicdirFCachedStaticPackSource extends PackSource {
	
	private final MusicdirPackSource musicdirsource;
	private final CachedStaticPackSource staticsource;
	
	public MusicdirFCachedStaticPackSource(MusicdirPackSource musicdirsource, CachedStaticPackSource staticsource) {
		this.musicdirsource = musicdirsource;
		this.staticsource = staticsource;
	}

	@Override
	public byte[] get(String id) {
		byte[] packbuf =  musicdirsource.get(id);
		if(packbuf == null) {
			packbuf = staticsource.get(null);
		}
		return packbuf;
	}

}
