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

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

enum LangOptions {loadmusic_usage,loadmusic_nopermission,loadmusic_nopermissionother,loadmusic_noconsoleselector,loadmusic_targetoffline,loadmusic_noplaylist,loadmusic_loaderunavilable,loadmusic_success,playmusic_usage,playmusic_nopermission,playmusic_nopermissionother,playmusic_noconsoleselector,playmusic_targetoffline,playmusic_noplaylist,playmusic_missingtrack,playmusic_playing,playmusic_success,playmusic_stopping,playmusic_stop,repeat_usage,repeat_nopermission,repeat_nopermissionother,repeat_noconsoleselector,repeat_targetoffline,repeat_unknownrepeattype,repeat_repeatall,repeat_repeatone,repeat_playall,repeat_playone;
	static {
		JavaPlugin plugin = JavaPlugin.getPlugin(AMusic.class);
		YamlConfiguration alang = null; 
		File langfile = new File(plugin.getDataFolder() + File.separator + "lang.yml");
		if(!langfile.exists()) {
			try {
				byte[] buf = new byte[2048];
				InputStream in = plugin.getResource(Bukkit.getServer().getClass().getPackage().getName().substring(23).equals("v1_16_R3") ? "lang_rgb.yml" : "lang_old.yml");
				if (in!=null) {
					buf = Arrays.copyOf(buf, in.read(buf));
					in.close();
					OutputStream out = new FileOutputStream(langfile);
					if(out!=null) {
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

		if(deffaultconfigurationsection!=null) {
			aavilablelocales = deffaultconfigurationsection.getKeys(false);
			aavilablelocales.add("default");
			for(LangOptions lang : values()) {
				lang.text = new HashMap<String,String>();
				for(String locale : aavilablelocales) {
					String msg = alang.getString(locale.concat(".").concat(lang.toString().replaceAll("_", ".")),"[{\"text\":\"ERROR LANG OPTION \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"".concat(lang.toString()).concat("\",\"bold\":true,\"color\":\"red\"},{\"text\":\" FOR LOCALE \",\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"").concat(locale).concat("\",\"bold\":true,\"color\":\"red\"},{\"text\":\" NOT EXSIST\",\"bold\":true,\"color\":\"dark_red\"}]"));
					lang.text.put(locale, msg);
				}
			}
		}
		avilablelocales = aavilablelocales;
	}
	private static final BaseComponent[] langnotloaded = ComponentSerializer.parse("{\"text\":\"ERROR LANG FILE NOT LOADED\",\"bold\":true,\"color\":\"dark_red\"}");
	private static final Set<String> avilablelocales;
	private Map<String,String> text = null;
	protected void sendMsg(CommandSender target,Placeholders...placeholders) {
		if(text==null) {
			target.sendMessage(langnotloaded);
			return;
		}
		String msg = text.get("default");
		if(target instanceof Player) {
			String locale = getLocale((Player)target);
			if(avilablelocales.contains(locale)) {
				msg = text.get(locale);
			}
		}
		if(!msg.isEmpty()) {
			for(Placeholders placeholder:placeholders) {
				msg = msg.replaceAll(placeholder.placeholder, placeholder.value);
			}
			target.sendMessage(ComponentSerializer.parse(msg));
		}
	}
	protected void sendMsgActionbar(Player target,Placeholders...placeholders) {
		if(text==null) {
			target.sendActionBar(ComponentSerializer.toString(langnotloaded));
			return;
		}
		String locale = getLocale(target);
		String msg = avilablelocales.contains(locale)?text.get(locale):text.get("default");
		if(!msg.isEmpty()) {
			for(Placeholders placeholder:placeholders) {
				msg = msg.replaceAll(placeholder.placeholder, placeholder.value);
			}
			target.sendActionBar(BaseComponent.toLegacyText(ComponentSerializer.parse(msg)));
		}
	}
	private static String getLocale(Player player) {
		switch (Bukkit.getServer().getClass().getPackage().getName().substring(23)) {
		case "v1_16_R3":
			return ((org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_15_R1":
			return ((org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_14_R1":
			return ((org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_13_R2":
			return ((org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_12_R1":
			return ((org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_11_R1":
			return ((org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_10_R1":
			return ((org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_9_R2":
			return ((org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		case "v1_8_R3":
			return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle().locale.toLowerCase();
		default:
			return player.getLocale().toLowerCase();
		}
	}
	protected static class Placeholders {
		protected final String placeholder;
		protected final String value;
		protected Placeholders(String placeholder,String value) {
			this.placeholder = placeholder;
			this.value = value;
		}
	}
}