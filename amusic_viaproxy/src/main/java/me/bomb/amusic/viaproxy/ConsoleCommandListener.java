package me.bomb.amusic.viaproxy;

import java.util.function.Consumer;

import me.bomb.amusic.util.Logger;
import me.bomb.amusic.viaproxy.command.Command;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;

public final class ConsoleCommandListener implements Consumer<ConsoleCommandEvent> {
	
	private final Logger logger;
	private final Command loadmusic, playmusic, playmusicuntrackable, repeat, uploadmusic;
	
	public ConsoleCommandListener(Logger logger, Command loadmusic, Command playmusic, Command playmusicuntrackable, Command repeat, Command uploadmusic) {
		this.logger = logger;
		this.loadmusic = loadmusic;
		this.playmusic = playmusic;
		this.playmusicuntrackable = playmusicuntrackable;
		this.repeat = repeat;
		this.uploadmusic = uploadmusic;
	}

	@Override
	public void accept(ConsoleCommandEvent event) {
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
