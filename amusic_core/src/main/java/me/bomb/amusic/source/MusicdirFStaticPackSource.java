package me.bomb.amusic.source;

public final class MusicdirFStaticPackSource extends PackSource {
	
	private final MusicdirPackSource musicdirsource;
	private final StaticPackSource staticsource;
	
	public MusicdirFStaticPackSource(MusicdirPackSource musicdirsource, StaticPackSource staticsource) {
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
