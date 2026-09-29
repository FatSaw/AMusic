package me.bomb.amusic.viaproxy;

import java.util.function.Consumer;

import me.bomb.amusic.util.Logger;
import me.bomb.amusic.viaproxy.command.Command;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;

public final class ConsoleCommandHandler implements Consumer<ConsoleCommandEvent> {
	
	private final Logger logger;
	private final Command loadmusic, playmusic, repeat;
	
	public ConsoleCommandHandler(Logger logger, Command loadmusic, Command playmusic, Command repeat) {
		this.logger = logger;
		this.loadmusic = loadmusic;
		this.playmusic = playmusic;
		this.repeat = repeat;
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
		if(cmd.equals("repeat")) {
			repeat.handleConsole(this.logger, event.getArgs());
			event.setCancelled(true);
			return;
		}
	}
	

}
