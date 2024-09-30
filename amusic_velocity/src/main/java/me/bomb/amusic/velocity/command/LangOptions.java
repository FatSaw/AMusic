package me.bomb.amusic.velocity.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.util.SimpleConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;

public enum LangOptions {
	loadmusic_usage, loadmusic_nopermission, loadmusic_nopermissionother, loadmusic_noconsoleselector, loadmusic_targetoffline, loadmusic_noplaylist, loadmusic_loaderunavilable, loadmusic_success, playmusic_usage, playmusic_nopermission, playmusic_nopermissionother, playmusic_noconsoleselector, playmusic_targetoffline, playmusic_noplaylist, playmusic_missingtrack, playmusic_success, playmusic_stop, repeat_usage, repeat_nopermission, repeat_nopermissionother, repeat_noconsoleselector, repeat_targetoffline, repeat_unknownrepeattype, repeat_repeatall, repeat_repeatone, repeat_playall, repeat_playone, repeat_random;
	
	private final Map<String, String> text = new HashMap<>();
	private static JSONComponentSerializer serializer;
	
	public static void loadLang(File langfile, boolean rgb) {
		serializer = JSONComponentSerializer.json();
		for (LangOptions lang : values()) {
			lang.text.clear();
		}
		byte[] buf = null;
		if (!langfile.exists()) {
			try {
				InputStream in = LangOptions.class.getClassLoader().getResourceAsStream(rgb ? "lang_rgb.yml" : "lang_old.yml");
				buf = new byte[0x2000];
				buf = Arrays.copyOf(buf, in.read(buf));
				in.close();
				OutputStream out = new FileOutputStream(langfile);
				if (out != null) {
					out.write(buf);
					out.close();
				}
			} catch (IOException e) {
			}
		} else {
			try {
				long filesize = langfile.length();
				if(filesize > 0x7FFFFFFD) {
					filesize = 0x7FFFFFFD;
				}
				FileInputStream in = new FileInputStream(langfile);
				buf = new byte[(int) filesize];
				int size = in.read(buf);
				if(size < filesize) {
					buf = Arrays.copyOf(buf, size);
				}
				in.close();
			} catch (IOException e) {
			}
		}
		SimpleConfiguration sc = new SimpleConfiguration(buf);
		String[] locales = sc.getSubKeys("");
		int i = locales.length;
		while(--i > -1) {
			String locale = locales[i], locale0 = locale.concat("\0");
			for (LangOptions lang : values()) {
				String optionpath = locale0.concat(lang.name().replace("_", "\0"));
				String msg = sc.getStringOrDefault(optionpath, optionpath);
				lang.text.put(locale, msg);
			}
		}
	}
	
	public void sendMsg(CommandSource source, Placeholders... placeholders) {
		String msg = null;
		if (source instanceof Player) {
			String locale = getLocale((Player) source);
			if (text.containsKey(locale)) {
				msg = text.get(locale);
			}
		}
		if(msg==null) {
			msg = text.get("default");
		}
		if (!msg.isEmpty()) {
			for (Placeholders placeholder : placeholders) {
				msg = msg.replace(placeholder.placeholder, placeholder.value);
			}
			char startchar = msg.charAt(0), endchar = msg.charAt(msg.length() - 1);
			if (startchar == '[' && endchar == ']' || startchar == '{' && endchar == '}') {
				Component component = serializer.deserialize(msg);
				if(component != null) {
					source.sendMessage(component);
					return;
				}
			}
			source.sendPlainMessage(msg);
		}
	}
	
	private static String getLocale(Player player) {
		Locale locale = player.getPlayerSettings().getLocale();
		if(locale==null) {
			return "default";
		}
		//locale.toLanguageTag();
		StringBuilder sb = new StringBuilder();
		sb.append(locale.getLanguage());
		sb.append("_");
		sb.append(locale.getCountry());
		return sb.toString().toLowerCase();
	}
	
	public static class Placeholders {
		protected final String placeholder;
		protected final String value;

		public Placeholders(String placeholder, String value) {
			this.placeholder = placeholder;
			this.value = value;
		}
	}

}
