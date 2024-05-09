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

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.bomb.amusic.SimpleConfiguration;
import me.bomb.amusic.bukkit.AMusicBukkit;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

enum LangOptions {
	loadmusic_usage, loadmusic_nopermission, loadmusic_nopermissionother, loadmusic_noconsoleselector, loadmusic_targetoffline, loadmusic_noplaylist, loadmusic_loaderunavilable, loadmusic_success, playmusic_usage, playmusic_nopermission, playmusic_nopermissionother, playmusic_noconsoleselector, playmusic_targetoffline, playmusic_noplaylist, playmusic_missingtrack, playmusic_success, playmusic_stop, repeat_usage, repeat_nopermission, repeat_nopermissionother, repeat_noconsoleselector, repeat_targetoffline, repeat_unknownrepeattype, repeat_repeatall, repeat_repeatone, repeat_playall, repeat_playone, repeat_random;
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusicBukkit.class);
		File langfile = new File(plugin.getDataFolder(), "lang.yml");
		byte[] buf = null;
		if (!langfile.exists()) {
			try {
				buf = new byte[8096];
				String nmsversion = Bukkit.getServer().getClass().getPackage().getName().substring(23);
				InputStream in = plugin.getResource(nmsversion.equals("v1_7_R4") || nmsversion.equals("v1_8_R3") || nmsversion.equals("v1_9_R2") || nmsversion.equals("v1_10_R1") || nmsversion.equals("v1_11_R1") || nmsversion.equals("v1_12_R1") || nmsversion.equals("v1_13_R2") || nmsversion.equals("v1_14_R1") || nmsversion.equals("v1_15_R1") ? "lang_old.yml" : "lang_rgb.yml");
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
				FileInputStream in = new FileInputStream(langfile);
				buf = new byte[in.available()];
				in.read(buf);
				in.close();
			} catch (IOException e) {
			}
		}
		SimpleConfiguration sc = new SimpleConfiguration(buf);
		String[] locales = sc.getSubKeys("");
		int i = locales.length;
		while(--i > -1) {
			String locale = locales[i];
			for (LangOptions lang : values()) {
				String optionpath = locale.concat(".").concat(lang.name().replace("_", "."));
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