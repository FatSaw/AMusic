package me.bomb.amusic;

import java.util.List;

final class PackInfo {
	protected final List<String> songs;
	protected final List<Short> lengths;
    protected PackInfo(List<String> songs,List<Short> lengths) {
    	this.songs = songs;
    	this.lengths = lengths;
    }
}