package me.bomb.amusic.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.bomb.amusic.MessageSender;

public enum LangOptions {
loadmusic_usage, loadmusic_nopermission, loadmusic_nopermissionother, loadmusic_noconsoleselector, loadmusic_targetoffline, loadmusic_processing, loadmusic_noplaylist, loadmusic_loaderunavilable, loadmusic_success_removed, loadmusic_success_packed, loadmusic_success_dispatched, loadmusic_unavilableselector_near, loadmusic_unavilableselector_random, loadmusic_unavilableselector_all, playmusic_usage, playmusic_nopermission, playmusic_nopermissionother, playmusic_noconsoleselector, playmusic_targetoffline, playmusic_noplaylist, playmusic_missingtrack, playmusic_success, playmusic_stop, playmusic_unavilableselector_near, playmusic_unavilableselector_random, playmusic_unavilableselector_all, repeat_usage, repeat_nopermission, repeat_nopermissionother, repeat_noconsoleselector, repeat_targetoffline, repeat_unknownrepeattype, repeat_repeatall, repeat_repeatone, repeat_playall, repeat_playone, repeat_random, repeat_unavilableselector_near, repeat_unavilableselector_random, repeat_unavilableselector_all, uploadmusic_usage, uploadmusic_nopermission, uploadmusic_nopermissiontoken, uploadmusic_disabled, uploadmusic_start_url_click, uploadmusic_start_url_show, uploadmusic_finish_player_notplayer, uploadmusic_finish_player_nosession, uploadmusic_finish_player_success, uploadmusic_finish_token_nosession, uploadmusic_finish_token_success, uploadmusic_finish_token_invalid;
	
	private static MessageSender messagesender;
	
	private final static String defaultlang = new String();
	
	public static void loadLang(MessageSender messagesender, File langfile, boolean rgb) {
		LangOptions.messagesender = messagesender;
		byte[] buf = null;
		if (!langfile.exists()) {
			try {
				buf = new byte[0x3000];
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
		String replacelangkey = "replacelang\0", localisationkey = "localisation\0", localisation = "default", localisation0 = localisationkey.concat(localisation).concat("\0");
		for (LangOptions lang : values()) {
			lang.text.clear();
			String langname = lang.name();
			String msg = sc.getStringOrDefault(localisation0.concat(langname.replace("_", "\0")), langname);
			msg = msg.replace("\\n", "\n");
			lang.text.put(defaultlang, msg);
		}
		String[] localisationkeys = sc.getSubKeys(localisationkey);
		int i = localisationkeys.length;
		while(--i > -1) {
			localisation = localisationkeys[i];
			if(localisation.equals("default")) continue;
			localisation0 = localisationkey.concat(localisation).concat("\0");
			for (LangOptions lang : values()) {
				String msg = sc.getStringOrDefault(localisation0.concat(lang.name().replace("_", "\0")), null);
				if(msg==null) continue;
				msg = msg.replace("\\n", "\n");
				lang.text.put(localisation, msg);
			}
		}
		String[] replacelangkeys = sc.getSubKeys(replacelangkey);
		i = replacelangkeys.length;
		while(--i > -1) {
			String to = replacelangkeys[i], replacekey0 = replacelangkey.concat(to);
			String from = sc.getStringOrDefault(replacekey0, to);
			for(LangOptions lang : values()) {
				lang.text.put(to, lang.text.get(from));
			}
		}
	}
	private final Map<String, String> text = new HashMap<String, String>();

	public void sendMsg(final Object target, final Placeholder... placeholders) {
		String msg = null;
		final String locale = messagesender.getLocale(target);
		if (locale != null) {
			msg = text.get(locale);
		}
		if(msg==null) {
			msg = text.get(defaultlang);
		}
		if (msg==null || msg.isEmpty()) {
			return;
		}
		int i = placeholders.length;
		while(--i > -1) {
			Placeholder placeholder = placeholders[i];
			msg = msg.replace(placeholder.placeholder, placeholder.value);
		}
		messagesender.send(target, msg);
	}
	
	public static class Placeholder {
		public final String placeholder;
		public final String value;

		public Placeholder(String placeholder, String value) {
			this.placeholder = placeholder;
			this.value = value;
		}
	}
}
