package me.bomb.amusic.bukkit.command;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bomb.amusic.bukkit.command.LangOptions.Placeholders;
import me.bomb.amusic.uploader.UploadManager;

public final class UploadmusicCommand implements CommandExecutor {
	
	private final UploadManager uploadmanager;
	private final ConcurrentHashMap<Player, UUID> uploaders = new ConcurrentHashMap<Player, UUID>();
	
	public UploadmusicCommand(UploadManager uploadmanager) {
		this.uploadmanager = uploadmanager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(uploadmanager == null) {
			LangOptions.uploadmusic_disabled.sendMsg(sender);
			return true;
		}
		if(!sender.hasPermission("amusic.uploadmusic")) {
			LangOptions.uploadmusic_nopermission.sendMsg(sender);
			return true;
		}
		if(args.length == 1 && "finish".equals(args[0])) {
			args[0] = args[0].toLowerCase();
			if(!(sender instanceof Player)) {
				LangOptions.uploadmusic_finish_player_notplayer.sendMsg(sender);
				return true;
			}
			Player player = (Player)sender;
			final UUID token = uploaders.remove(player);
			if(token == null || !uploadmanager.endSession(token)) {
				LangOptions.uploadmusic_finish_player_nosession.sendMsg(sender);
				return true;
			}
			LangOptions.uploadmusic_finish_player_success.sendMsg(sender);
			return true;
		}
		if(args.length < 2) {
			LangOptions.uploadmusic_usage.sendMsg(sender);
			return true;
		}
		args[0] = args[0].toLowerCase();
		if("start".equals(args[0])) {
			if (args.length > 1) {
				if (args.length > 2) {
					StringBuilder sb = new StringBuilder(args[1]);
					for(int i = 2;i < args.length;++i) {
						sb.append(' ');
						sb.append(args[i]);
					}
					args[1] = sb.toString();
				}
			}
			final UUID token = uploadmanager.generateToken(args[1]);
			String url = uploadmanager.uploaderhost.concat(token.toString());
			(sender instanceof Player ? LangOptions.uploadmusic_start_url_click : LangOptions.uploadmusic_start_url_show).sendMsg(sender, new Placeholders("%url%", url));
			if(!(sender instanceof Player)) {
				return true;
			}
			Player player = (Player)sender;
			uploaders.put(player, token);
			return true;
		}
		if("finish".equals(args[0])) {
			if(!sender.hasPermission("amusic.uploadmusic.token")) {
				LangOptions.uploadmusic_nopermissiontoken.sendMsg(sender);
			}
			try {
				final UUID token = UUID.fromString(args[1]);
				(uploadmanager.endSession(token) ? LangOptions.uploadmusic_finish_token_success : LangOptions.uploadmusic_finish_token_nosession).sendMsg(sender);
			} catch(IllegalArgumentException ex) {
				LangOptions.uploadmusic_finish_token_invalid.sendMsg(sender);
			}
			return true;
		}
		LangOptions.uploadmusic_usage.sendMsg(sender);
		return true;
	}
	
	public void logoutUploader(Player uploader) {
		final UUID token = uploaders.remove(uploader);
		if(token == null) {
			return;
		}
		uploadmanager.endSession(token);
	}
	
}
