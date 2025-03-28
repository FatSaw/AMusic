package me.bomb.amusic.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.HashSet;

/*
 * Read only configuration
 */
public final class SimpleConfiguration {
	
	private final static Decoder base64decoder;
	
	static {
		base64decoder = Base64.getDecoder();
	}
	
	private final HashMap<String,String> kv;
	
	public SimpleConfiguration(byte[] bytes) {
		HashMap<String,String> kv = new HashMap<>();
		int i = bytes.length;
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
		if(maxlevel<level) maxlevel = level;
		++maxlevel;
		i = intlist.size();
		byte[] levels = new byte[i];
		byte[][] strs = new byte[i][];
		int pstrid = 0;
		while(--i>-1) {
			int strid = intlist.get(i);
			levels[i] = level;
			int strsize = -level;
			level = (byte) strid;
			strid>>=8;
			strsize+=strid;
			strsize-=pstrid;
			byte[] strbytes = new byte[strsize];
			pstrid = strid;
			while(--strsize>-1) {
				strbytes[strsize] = bytes[--strid];
			}
			strs[i] = strbytes;
		}
		i = strs.length;
		byte plevel = 0;
		String[] pkey = new String[maxlevel];
		++levels[i-1];
		while(--i>-1) {
			level = levels[i];
			byte[] str = strs[i];
			int valuekeysplit = 0;
			while(valuekeysplit<str.length) {
				if(str[valuekeysplit] == 0x3A) {
					break;
				}
				++valuekeysplit;
			}
			if(str.length == 0 || str[0] == 0x23) continue;
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
					fullkey.append('\0');
					fullkey.append(apk);
				}
				fullkey.delete(0, 1);
				++valuekeysplit;
				boolean hasvalue = valuekeysplit < str.length && str[valuekeysplit] == 0x20;
				String value = hasvalue&&++valuekeysplit != str.length ? new String(Arrays.copyOfRange(str, valuekeysplit, str.length)) : "";
				kv.put(fullkey.toString(), value);
			}
		}
		this.kv = kv;
	}
	
	private static void appendKey(String key, StringBuilder sb) {
		key = key.replace('\0', '_');
		sb.append("Missing config option: ");
		sb.append(key);
		sb.append('\n');
	}
	
	public boolean hasKey(String key) {
		return kv.containsKey(key);
	}
	
	public String getStringOrDefault(String key, String defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		return value;
	}
	public String getString(String key) throws NullPointerException {
		String value = kv.get(key);
		if(value==null) throw new NullPointerException();
		return value;
	}
	public String getStringOrError(String key, StringBuilder sb) {
		String value = kv.get(key);
		if(value==null) {
			appendKey(key, sb);
			return null;
		}
		return value;
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
	public boolean getBoolean(String key) throws NullPointerException, IllegalArgumentException {
		String value = kv.get(key);
		if(value==null) throw new NullPointerException();
		if(value.equals("true")) {
			return true;
		} else if(value.equals("false")) {
			return false;
		}
		throw new IllegalArgumentException();
	}
	public boolean getBooleanOrError(String key, StringBuilder sb) {
		String value = kv.get(key);
		if(value==null) {
			appendKey(key, sb);
			return false;
		}
		if(value.equals("true")) {
			return true;
		} else if(value.equals("false")) {
			return false;
		}
		appendKey(key, sb);
		return false;
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
	public int getInt(String key) throws NullPointerException, NumberFormatException {
		String value = kv.get(key);
		if(value==null) throw new NullPointerException();
		return Integer.parseInt(value);
	}
	public int getIntOrError(String key, StringBuilder sb) {
		String value = kv.get(key);
		if(value==null) {
			appendKey(key, sb);
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			appendKey(key, sb);
			return 0;
		}
	}
	
	public float getFloatOrDefault(String key, float defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return defaultvalue;
		}
	}
	public float getFloat(String key) throws NullPointerException, NumberFormatException {
		String value = kv.get(key);
		if(value==null) throw new NullPointerException();
		return Float.parseFloat(value);
	}
	public float getFloatOrError(String key, StringBuilder sb) {
		String value = kv.get(key);
		if(value==null) {
			appendKey(key, sb);
			return 0;
		}
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			appendKey(key, sb);
			return 0;
		}
	}
	
	public byte[] getBytesBase64OrDefault(String key, byte[] defaultvalue) {
		String value = kv.get(key);
		if(value==null) return defaultvalue;
		try {
			return base64decoder.decode(value);
		} catch (IllegalArgumentException e) {
			return defaultvalue;
		}
	}
	public byte[] getBytesBase64(String key) throws NullPointerException, IllegalArgumentException {
		String value = kv.get(key);
		if(value==null) throw new NullPointerException();
		return base64decoder.decode(value);
	}
	public byte[] getBytesBase64OrError(String key, StringBuilder sb) {
		String value = kv.get(key);
		if(value==null) {
			appendKey(key, sb);
			return null;
		}
		try {
			return base64decoder.decode(value);
		} catch (IllegalArgumentException e) {
			appendKey(key, sb);
			return null;
		}
	}
	
	public String[] getSubKeys(String parentsection) {
		HashSet<String> keys = new HashSet<>();
		int parentsectionlength = parentsection.length();
		for(String key : kv.keySet()) {
			int keylength = key.length();
			if(keylength < parentsectionlength || !key.startsWith(parentsection)) {
				continue;
			}
			int sectionend = key.indexOf('\0', parentsectionlength);
			if(sectionend == -1) {
				sectionend = keylength;
			}
			keys.add(key.substring(parentsectionlength, sectionend));

		}
		return keys.toArray(new String[keys.size()]);
	}
}
