package me.bomb.amusic.viaproxy;

import me.bomb.amusic.viaproxy.command.Command;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;

public final class ConsoleCommandListener {
	
	private final org.apache.logging.log4j.Logger logger;
	private final Command loadmusic, playmusic, playmusicuntrackable, repeat, uploadmusic;
	
	public ConsoleCommandListener(org.apache.logging.log4j.Logger logger, Command loadmusic, Command playmusic, Command playmusicuntrackable, Command repeat, Command uploadmusic) {
		this.logger = logger;
		this.loadmusic = loadmusic;
		this.playmusic = playmusic;
		this.playmusicuntrackable = playmusicuntrackable;
		this.repeat = repeat;
		this.uploadmusic = uploadmusic;
	}
	
	@EventHandler
	public void onConsoleCommand(ConsoleCommandEvent event) {
		String cmd = event.getCommand().toLowerCase();
		if(cmd.equals("loadmusic")) {
			loadmusic.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
		if(cmd.equals("playmusic")) {
			playmusic.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
		if(cmd.equals("playmusicuntrackable")) {
			playmusicuntrackable.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
		if(cmd.equals("repeat")) {
			repeat.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
		if(cmd.equals("uploadmusic")) {
			uploadmusic.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
	}
	

}
