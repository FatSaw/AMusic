package me.bomb.amusic.bukkit.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.util.SimpleConfiguration;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public enum LangOptions {
	loadmusic_usage, loadmusic_nopermission, loadmusic_nopermissionother, loadmusic_noconsoleselector, loadmusic_targetoffline, loadmusic_noplaylist, loadmusic_loaderunavilable, loadmusic_success, loadmusic_unavilableselector_near, loadmusic_unavilableselector_random, loadmusic_unavilableselector_all, playmusic_usage, playmusic_nopermission, playmusic_nopermissionother, playmusic_noconsoleselector, playmusic_targetoffline, playmusic_noplaylist, playmusic_missingtrack, playmusic_success, playmusic_stop, playmusic_unavilableselector_near, playmusic_unavilableselector_random, playmusic_unavilableselector_all, repeat_usage, repeat_nopermission, repeat_nopermissionother, repeat_noconsoleselector, repeat_targetoffline, repeat_unknownrepeattype, repeat_repeatall, repeat_repeatone, repeat_playall, repeat_playone, repeat_random, repeat_unavilableselector_near, repeat_unavilableselector_random, repeat_unavilableselector_all;
	public static void loadLang(File langfile, boolean rgb) {
		byte[] buf = null;
		if (!langfile.exists()) {
			try {
				buf = new byte[0x2000];
				InputStream in = LangOptions.class.getClassLoader().getResourceAsStream(rgb ? "lang_rgb.yml" : "lang_old.yml");
				if (in != null) {
					buf = Arrays.copyOf(buf, in.read(buf));
					in.close();
					OutputStream out = new FileOutputStream(langfile);
					if (out != null) {
						out.write(buf);
						out.close();
					}
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
				if (!msg.isEmpty()) {
					char startchar = msg.charAt(0), endchar = msg.charAt(msg.length() - 1);
					if (startchar == '[' && endchar == ']' || startchar == '{' && endchar == '}') {
						msg = TextComponent.toLegacyText(ComponentSerializer.parse(msg));
					}
				}
				lang.text.put(locale, msg);
			}
		}
	}
	private final Map<String, String> text = new HashMap<String, String>();

	public void sendMsg(CommandSender target, Placeholders... placeholders) {
		String msg = null;
		if (target instanceof Player) {
			String locale = getLocale((Player) target);
			msg = text.get(locale);
		}
		if(msg==null) {
			msg = text.get("default");
		}
		if (!msg.isEmpty()) {
			for (Placeholders placeholder : placeholders) {
				msg = msg.replace(placeholder.placeholder, placeholder.value);
			}
			target.sendMessage(msg);
		}
	}

	private static String getLocale(Player player) {
		try {
			return player.getLocale().toLowerCase();
		} catch (NoSuchMethodError e) {
			return player.spigot().getLocale().toLowerCase();
		}
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