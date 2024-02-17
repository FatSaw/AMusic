package me.bomb.amusic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public enum LangOptions {
	loadmusic_usage, loadmusic_nopermission, loadmusic_nopermissionother, loadmusic_noconsoleselector, loadmusic_targetoffline, loadmusic_noplaylist, loadmusic_loaderunavilable, loadmusic_success, playmusic_usage, playmusic_nopermission, playmusic_nopermissionother, playmusic_noconsoleselector, playmusic_targetoffline, playmusic_noplaylist, playmusic_missingtrack, playmusic_playing, playmusic_success, playmusic_stopping, playmusic_stop, repeat_usage, repeat_nopermission, repeat_nopermissionother, repeat_noconsoleselector, repeat_targetoffline, repeat_unknownrepeattype, repeat_repeatall, repeat_repeatone, repeat_playall, repeat_playone, repeat_random;
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusic.class);
		YamlConfiguration alang = null;
		File langfile = new File(plugin.getDataFolder(), "lang.yml");
		if (!langfile.exists()) {
			try {
				byte[] buf = new byte[8096];
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
		}
		alang = YamlConfiguration.loadConfiguration(langfile);
		ConfigurationSection deffaultconfigurationsection = alang.getConfigurationSection("");
		Set<String> aavilablelocales = null;

		if (deffaultconfigurationsection != null) {
			aavilablelocales = deffaultconfigurationsection.getKeys(false);
			aavilablelocales.add("default");
			for (LangOptions lang : values()) {
				for (String locale : aavilablelocales) {
					String optionpath = locale.concat(".").concat(lang.name().replaceAll("_", "."));
					String msg = alang.getString(optionpath);
					if (!msg.isEmpty()) {
						//msg = TextComponent.toLegacyText(ComponentSerializer.parse("[{\"text\":\"ERROR LANG OPTION \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"".concat(lang.name()).concat("\",\"bold\":true,\"color\":\"red\"},{\"text\":\" FOR LOCALE \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"").concat(locale).concat("\",\"bold\":true,\"color\":\"red\"},{\"text\":\" NOT EXSIST\",\"bold\":true,\"color\":\"dark_red\"}]")));
						char startchar = msg.charAt(0), endchar = msg.charAt(msg.length() - 1);
						if (startchar == '[' && endchar == ']' || startchar == '{' && endchar == '}') {
							msg = TextComponent.toLegacyText(ComponentSerializer.parse(msg));
						}
					}
					lang.text.put(locale, msg);
				}
			}
		}
		avilablelocales = aavilablelocales;
	}
	private static final Set<String> avilablelocales;
	private final Map<String, String> text = new HashMap<String, String>();

	public void sendMsg(CommandSender target, Placeholders... placeholders) {
		String msg = text.get("default");
		if (target instanceof Player) {
			String locale = getLocale((Player) target);
			if (avilablelocales.contains(locale)) {
				msg = text.get(locale);
			}
		}
		if (!msg.isEmpty()) {
			for (Placeholders placeholder : placeholders) {
				msg = msg.replaceAll(placeholder.placeholder, placeholder.value);
			}
			target.sendMessage(msg);
		}
	}

	public void sendMsgActionbar(Player target, Placeholders... placeholders) {
		String locale = getLocale(target);
		String msg = avilablelocales.contains(locale) ? text.get(locale) : text.get("default");
		if (!msg.isEmpty()) {
			for (Placeholders placeholder : placeholders) {
				msg = msg.replaceAll(placeholder.placeholder, placeholder.value);
			}
			try {
				target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
			} catch (NoSuchMethodError | NoClassDefFoundError e) {
				target.sendMessage(msg);
			}
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