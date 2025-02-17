package me.bomb.amusic.velocity.command;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.uploader.UploadManager;
import me.bomb.amusic.util.LangOptions;
import me.bomb.amusic.util.LangOptions.Placeholder;

public final class UploadmusicCommand implements SimpleCommand {

	private final AMusic amusic;
	private final String uploaderhost;
	private final ConcurrentHashMap<Player, UUID> uploaders = new ConcurrentHashMap<Player, UUID>();
	
	public UploadmusicCommand(AMusic amusic, String uploaderhost) {
		this.amusic = amusic;
		this.uploaderhost = uploaderhost;
	}
	
	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		if(uploaderhost == null) {
			LangOptions.uploadmusic_disabled.sendMsg(sender);
			return;
		}
		if(!sender.hasPermission("amusic.uploadmusic")) {
			LangOptions.uploadmusic_nopermission.sendMsg(sender);
			return;
		}
		String[] args = invocation.arguments();
		if(args.length == 1 && "finish".equals(args[0])) {
			args[0] = args[0].toLowerCase();
			if(!(sender instanceof Player)) {
				LangOptions.uploadmusic_finish_player_notplayer.sendMsg(sender);
				return;
			}
			Player player = (Player)sender;
			final UUID token = uploaders.remove(player);
			if(token == null || !amusic.closeUploadSession(token, true)) {
				LangOptions.uploadmusic_finish_player_nosession.sendMsg(sender);
				return;
			}
			LangOptions.uploadmusic_finish_player_success.sendMsg(sender);
			return;
		}
		if(args.length < 2) {
			LangOptions.uploadmusic_usage.sendMsg(sender);
			return;
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
			final UUID token = amusic.openUploadSession(args[1]);
			String url = uploaderhost.concat(token.toString());
			(sender instanceof Player ? LangOptions.uploadmusic_start_url_click : LangOptions.uploadmusic_start_url_show).sendMsg(sender, new Placeholder("%url%", url));
			if(!(sender instanceof Player)) {
				return;
			}
			Player player = (Player)sender;
			uploaders.put(player, token);
			return;
		}
		if("finish".equals(args[0])) {
			if(!sender.hasPermission("amusic.uploadmusic.token")) {
				LangOptions.uploadmusic_nopermissiontoken.sendMsg(sender);
			}
			try {
				final UUID token = UUID.fromString(args[1]);
				(amusic.closeUploadSession(token, true) ? LangOptions.uploadmusic_finish_token_success : LangOptions.uploadmusic_finish_token_nosession).sendMsg(sender);
			} catch(IllegalArgumentException ex) {
				LangOptions.uploadmusic_finish_token_invalid.sendMsg(sender);
			}
			return;
		}
		LangOptions.uploadmusic_usage.sendMsg(sender);
		return;
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource sender = invocation.source();
		if (!sender.hasPermission("amusic.uploadmusic")) {
			return null;
		}
		String[] args = invocation.arguments();
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			String arg0 = args[0].toLowerCase();
			if ("start".startsWith(arg0)) {
				tabcomplete.add("start");
			}
			if ("finish".startsWith(arg0)) {
				tabcomplete.add("finish");
			}
		}
		if (args.length == 2 && sender.hasPermission("amusic.uploadmusic.token")) {
			String arg0 = args[0].toLowerCase();
			if ("finish".equals(arg0)) {
				UUID[] sessions = amusic.getUploadSessions();
				String arg1 = args[1].toUpperCase();
				for(UUID token : sessions) {
					String tokenstr = token.toString();
					if(tokenstr.startsWith(arg1)) {
						tabcomplete.add(tokenstr);
					}
				}
			}
		}
		return tabcomplete;
	}
	
	public void logoutUploader(Player uploader) {
		final UUID token = uploaders.remove(uploader);
		if(token == null) {
			return;
		}
		amusic.closeUploadSession(token, false);
	}
	
}
