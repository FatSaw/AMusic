package me.bomb.amusic.bukkit.command;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.bomb.amusic.uploader.UploadManager;

public final class UploadmusicCommand implements CommandExecutor {
	
	private final UploadManager uploadmanager;
	
	public UploadmusicCommand(UploadManager uploadmanager) {
		this.uploadmanager = uploadmanager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(uploadmanager == null) {
			sender.sendMessage("Loader disabled");
			return true;
		}
		if(!sender.hasPermission("amusic.uploadmusic")) {
			sender.sendMessage("No permission!");
			return true;
		}
		if(args.length < 2) {
			sender.sendMessage("Usage: /uploadmusic <start/finish> <playlist/token>");
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
			String url = uploadmanager.uploaderhost.concat(uploadmanager.generateToken(args[1]).toString());
			sender.sendMessage("URL: " + url);
			return true;
		}
		if("finish".equals(args[0])) {
			try {
				UUID token = UUID.fromString(args[1]);
				sender.sendMessage(uploadmanager.endSession(token) ? "Token removed" : "Token invalid!");
			} catch(IllegalArgumentException ex) {
				sender.sendMessage("Token format invalid!");
			}
			return true;
		}
		sender.sendMessage("Usage: /uploadmusic <start/finish> <playlist/token>");
		return true;
	}
	
}
