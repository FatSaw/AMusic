package me.bomb.amusic.permission;

public enum AMusicPermission {
	
	LOADMUSIC("amusic.loadmusic", (byte) 0x01), LOADMUSIC_OTHER("amusic.loadmusic.other", (byte) 0x02), LOADMUSIC_UPDATE("amusic.loadmusic.update", (byte) 0x03), PLAYMUSIC("amusic.playmusic", (byte) 0x04), PLAYMUSIC_OTHER("amusic.playmusic.other", (byte) 0x05), REPEAT("amusic.repeat", (byte) 0x06), REPEAT_OTHER("amusic.repeat.other", (byte) 0x07), UPLOADMUSIC("amusic.uploadmusic", (byte) 0x08), UPLOADMUSIC_TOKEN("amusic.uploadmusic.token", (byte) 0x09);
	
	public final String permission;
	protected final byte id;
	
	private AMusicPermission(String permission, byte id) {
		this.permission = permission;
		this.id = id;
	}

}
