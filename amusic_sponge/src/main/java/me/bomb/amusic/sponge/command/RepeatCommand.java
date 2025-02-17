package me.bomb.amusic.sponge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Server;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import me.bomb.amusic.AMusic;
import me.bomb.amusic.RepeatType;
import me.bomb.amusic.util.LangOptions;

public final class RepeatCommand implements CommandCallable {
	private final Server server;
	private final AMusic amusic;
	public RepeatCommand(Server server, AMusic amusic) {
		this.server = server;
		this.amusic = amusic;
	}
	
	private void executeCommand(CommandSource source, String repeattype, UUID... targets) {
		switch (repeattype) {
		case "playone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(source);
					return;
				}
				amusic.setRepeatMode(target, null);
			}
			LangOptions.repeat_playone.sendMsg(source);
			return;
		case "repeatone":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(source);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATONE);
			}
			LangOptions.repeat_repeatone.sendMsg(source);
			return;
		case "repeatall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(source);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.REPEATALL);
			}
			LangOptions.repeat_repeatall.sendMsg(source);
			return;
		case "playall":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(source);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.PLAYALL);
			}
			
			LangOptions.repeat_playall.sendMsg(source);
			return;
		case "random":
			for(UUID target : targets) {
				if (target == null) {
					LangOptions.repeat_targetoffline.sendMsg(source);
					return;
				}
				amusic.setRepeatMode(target, RepeatType.RANDOM);
			}
			LangOptions.repeat_random.sendMsg(source);
			return;
		default:
			LangOptions.repeat_unknownrepeattype.sendMsg(source);
		}
		
	}
	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		if (!source.hasPermission("amusic.repeat")) {
			LangOptions.repeat_nopermission.sendMsg(source);
			return CommandResult.success();
		}
		String[] args = arguments.split(" ", 127);
		if (args.length > 1) {
			if (args[0].equals("@s")) {
				if (source instanceof Player) {
					args[0] = ((Player) source).getName();
				} else {
					LangOptions.repeat_noconsoleselector.sendMsg(source);
					return CommandResult.success();
				}
			} else if (!source.hasPermission("amusic.repeat.other")) {
				LangOptions.repeat_nopermissionother.sendMsg(source);
				return CommandResult.success();
			}
			
			Optional<Player> otarget = server.getPlayer(args[0]);
			if (!otarget.isPresent()) {
				LangOptions.repeat_targetoffline.sendMsg(source);
				return CommandResult.success();
			}
			Player target = otarget.get();
			this.executeCommand(source, args[1].toLowerCase(), target.getUniqueId());
			
		} else {
			LangOptions.repeat_usage.sendMsg(source);
		}
		return CommandResult.success();
	}
	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition) throws CommandException {
		if (!source.hasPermission("amusic.repeat")) {
			return null;
		}
		String[] args = arguments.split(" ", 127);
		ArrayList<String> tabcomplete = new ArrayList<String>();
		if (args.length == 1) {
			if (source instanceof Player) {
				tabcomplete.add("@s");
			}
			if (source.hasPermission("amusic.repeat.other")) {
				for (Player player : server.getOnlinePlayers()) {
					if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
						tabcomplete.add(player.getName());
					}
				}
			}
		}
		if (args.length == 2) {
			String arg1 = args[1].toLowerCase();
			if ("repeatall".startsWith(arg1)) {
				tabcomplete.add("repeatall");
			}
			if ("repeatone".startsWith(arg1)) {
				tabcomplete.add("repeatone");
			}
			if ("playone".startsWith(arg1)) {
				tabcomplete.add("playone");
			}
			if ("playall".startsWith(arg1)) {
				tabcomplete.add("playall");
			}
			if ("random".startsWith(arg1)) {
				tabcomplete.add("random");
			}
		}
		return tabcomplete;
	}
	@Override
	public boolean testPermission(CommandSource source) {
		return true;
	}
	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		return Optional.empty();
	}
	@Override
	public Optional<Text> getHelp(CommandSource source) {
		return Optional.empty();
	}
	@Override
	public Text getUsage(CommandSource source) {
		return null;
	}
}
