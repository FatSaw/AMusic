package me.bomb.amusic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Map.Entry;

public final class SimpleConfiguration {
	
	private final static Decoder base64decoder;
	
	static {
		base64decoder = Base64.getDecoder();
	}
	
	private final HashMap<String,String> kv;
	
	public SimpleConfiguration(byte[] bytes) {
		HashMap<String,String> kv = new HashMap<>();
		int pstrid = bytes.length;
		int i = pstrid;
		byte level = 0, maxlevel = 0;
		ArrayList<Integer> intlist = new ArrayList<Integer>();
		while(--i>-1) {
			byte ch = bytes[i];
			if(ch == 0x0A) {
				++level;
				if(maxlevel<level) maxlevel = level;
				int tadd = ((i << 8) | level);
				intlist.add(tadd);
				level = 0;
			} else if(ch == 0x20) {
				++level;
			} else {
				level = 0;
			}
		}
		intlist.add((int)level);
		if(maxlevel<level) maxlevel = level;
		++maxlevel;
		i = intlist.size();
		++i;
		byte[] levels = new byte[i];
		byte[][] strs = new byte[i][];
		strs[0] = levels;
		int j = 0;
		while(--i>0) {
			int strid = intlist.get(j);
			++j;
			byte ch = (byte) strid;
			levels[i] = ch;
			strid>>=8;
			--pstrid;
			int strsize = pstrid - strid;
			strsize-=ch;
			byte[] strbytes = new byte[strsize];
			while(--strsize>-1) {
				strbytes[strsize] = bytes[--pstrid];
			}
			strs[i] = strbytes;
			pstrid = strid;
		}
		levels[1] = ++level;
		
		
		
		i = 0;
		byte plevel = 0;
		String[] pkey = new String[maxlevel];
		while(++i<strs.length) {
			level = levels[i];
			byte[] str = strs[i];
			int valuekeysplit = 0;
			while(valuekeysplit<str.length) {
				if(str[valuekeysplit] == 0x3A) {
					break;
				}
				++valuekeysplit;
			}
			if(str[0] == 0x23) continue;
			if(valuekeysplit != str.length) {
				String key = new String(Arrays.copyOf(str, valuekeysplit));
				StringBuilder fullkey = new StringBuilder();
				if(level>plevel) {
					pkey[level] = key;
					plevel = level;
				} else if(level<plevel) {
					pkey[level] = key;
					for(byte k = plevel;level<k;--k) {
						pkey[k] = null;
					}
					plevel = level;
				} else {
					pkey[level] = key;
				}
				++level;
				for(byte k = 1; k < level;++k) {
					String apk = pkey[k];
					if(apk==null) continue;
					fullkey.append('.');
					fullkey.append(apk);
				}
				fullkey.delete(0, 1);
				++valuekeysplit;
				boolean hasvalue = valuekeysplit < str.length && str[valuekeysplit] ==  0x20;
				String value = hasvalue&&++valuekeysplit != str.length ? new String(Arrays.copyOfRange(str, valuekeysplit, str.length)) : "";
				kv.put(fullkey.toString(), value);
			}
		}
		this.kv = kv;
	}
	
	public boolean getBooleanOrDefault(String key, boolean defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		if(value.equals("true")) {
			return true;
		} else if(value.equals("false")) {
			return false;
		}
		return defaultvalue;
	}
	public int getIntOrDefault(String key, int defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultvalue;
		}
	}
	public String getStringOrDefault(String key, String defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		return value;
	}
	public byte[] getBytesBase64OrDefault(String key, byte[] defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		try {
			return base64decoder.decode(value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return defaultvalue;
		}
	}
	public HashMap<String,String> getSub(String key) {
		HashMap<String,String> map = new HashMap<String,String>();
		byte keylvl = 0;
		while(keylvl!=-128 && key.indexOf(0xE2, keylvl)!=-1) {
			++keylvl;
		}
		++keylvl;
		for(Entry<String,String> entry : kv.entrySet()) {
			String ekey = entry.getKey();
			byte ekeylvl = 0;
			while(ekeylvl!=-128 && ekey.indexOf(0xE2, ekeylvl)!=-1) {
				++ekeylvl;
			}
			if(keylvl==ekeylvl&&ekey.startsWith(key)) {
				map.put(ekey, entry.getValue());
			}
		}
		return map;
	}
	public HashMap<String,String> getStartsWith(String key) {
		HashMap<String,String> map = new HashMap<String,String>();
		for(Entry<String,String> entry : kv.entrySet()) {
			String ekey = entry.getKey();
			if(ekey.startsWith(key)) {
				map.put(ekey, entry.getValue());
			}
		}
		return map;
	}
	public HashMap<String,String> getLevel(byte level) {
		HashMap<String,String> map = new HashMap<String,String>();
		for(Entry<String,String> entry : kv.entrySet()) {
			String ekey = entry.getKey();
			byte ekeylvl = 0;
			while(ekeylvl!=-128 && ekey.indexOf(0xE2, level)!=-1) {
				++ekeylvl;
			}
			if(level==ekeylvl) {
				map.put(ekey, entry.getValue());
			}
		}
		return map;
	}
}
