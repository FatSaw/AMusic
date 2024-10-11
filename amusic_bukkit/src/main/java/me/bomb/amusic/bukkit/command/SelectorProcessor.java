package me.bomb.amusic.bukkit.command;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;

public final class SelectorProcessor {
	
	private final Server server;
	private final Random random;
	
	public SelectorProcessor(Server server, Random random) {
		this.server = server;
		this.random = random;
	}
	
	protected UUID getNearest(CommandSender sender, String selectorarg) {
		Location executorlocation;
		if((executorlocation = getSenderLocation(sender)) == null) {
			return null;
		}
		World world = executorlocation.getWorld();
		Player[] players;
		{
			List<Player> aplayers = world.getPlayers();
			players = aplayers.toArray(new Player[aplayers.size()]);
		}
		String[] args = getSelectorArgs(selectorarg);
		filterPosition(players, args);
		double[] distances = filterDistance(players, args, executorlocation);
		sortNull(players, distances);
		int i = players.length;
		while(--i > -1 && players[i] == null);
		int j = i;
		double distance = Double.MAX_VALUE;
		while(--i > -1) {
			if(distance < distances[i]) continue;
			distance = distances[i];
			j = i;
		}
		return players[j].getUniqueId();
	}
	
	protected UUID getRandom(CommandSender sender, String selectorarg) {
		Location executorlocation;
		if((executorlocation = getSenderLocation(sender)) == null) {
			return null;
		}
		World world = executorlocation.getWorld();
		Player[] players;
		{
			List<Player> aplayers = world.getPlayers();
			players = aplayers.toArray(new Player[aplayers.size()]);
		}
		String[] args = getSelectorArgs(selectorarg);
		filterPosition(players, args);
		filterDistance(players, args, executorlocation);
		sortNull(players);
		int i = players.length;
		while(--i > -1 && players[i] == null);
		return players[random.nextInt(i)].getUniqueId();
	}
	
	protected UUID[] getSameWorld(CommandSender sender, String selectorarg) {
		Location executorlocation;
		if((executorlocation = getSenderLocation(sender)) == null) {
			return null;
		}
		World world = executorlocation.getWorld();
		Player[] players;
		{
			List<Player> aplayers = world.getPlayers();
			players = aplayers.toArray(new Player[aplayers.size()]);
		}
		String[] args = getSelectorArgs(selectorarg);
		filterPosition(players, args);
		
		double[] distances = filterDistance(players, args, executorlocation);
		filterCountDistance(players, distances, args, random);
		
		filterRandom(players, args, random);
		sortNull(players, distances);
		int i = players.length;
		while(--i > -1 && players[i] == null);
		UUID[] uuids = new UUID[i];
		while(--i > -1) {
			uuids[i] = players[i].getUniqueId();
		}
		return uuids;
	}
	
	/*
	 * Maximal 17/165
	 * Max separator count = 0
	 * Max arg = 17
	 */
	private static final void filterRandom(Player[] players, String[] args, Random random) {
		int i = args.length, j = players.length;
		if (i == 0 || j == 0) return;
		while(--i > -1) {
			if(args[i] == null) continue;
			if(args[i].startsWith("random=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(7);
				if(valuestr.isEmpty()) continue;
				int value;
				try {
					value = Integer.parseInt(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				int k = 0;
				while(--j > -1) {
					if(players[j] == null) continue;
					++k;
				}
				if(k<=value) {
					continue;
				}
				k-=value; 
				while(--k > -1) {
					continue;
				}
				int[] toremove = unrepeatableRandom(random, players.length, k);
				j = toremove.length;
				while(--j > -1) {
					players[j] = null;
				}
				continue;
			}
		}
	}
	
	/*
	 * Maximal 18/165
	 * Max separator count = 0
	 * Max arg = 18
	 */
	// TODO Sorting by distance with same value randomization
	private static final void filterCountDistance(Player[] players, double[] distances, String[] args, Random random) {
		int i = args.length, j = players.length;
		if (i == 0 || j == 0 || distances.length == 0) return;
		while(--i > -1) {
			if(args[i] == null) continue;
			if(args[i].startsWith("closer=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(7);
				if(valuestr.isEmpty()) continue;
				int value;
				try {
					value = Integer.parseInt(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				int k = 0;
				while(--j > -1) {
					if(players[j] == null || distances[j] == Double.NaN) continue;
					++k;
				}
				
				continue;
			}
			if(args[i].startsWith("further=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(8);
				if(valuestr.isEmpty()) continue;
				int value;
				try {
					value = Integer.parseInt(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				int k = 0;
				while(--j > -1) {
					if(players[j] == null || distances[j] == Double.NaN) continue;
					++k;
				}
				
				continue;
			}
		}
	}
	
	private static final void sortNull(Object[] object) {
		int i = object.length, j = -1;
		while(true) {
			while(++j < i && object[j] != null); //found first null
			while(--i > j && object[i] == null); //found last not null
			if(j>i) return;
			object[j] = object[i];
			object[i] = null;
		}
	}
	
	private static final void sortNull(Object[] object, double[] distance) {
		int i = object.length, j = -1;
		while(true) {
			while(++j < i && object[j] != null); //found first null
			while(--i > j && object[i] == null); //found last not null
			if(j>i) return;
			object[j] = object[i];
			distance[j] = distance[i];
			object[i] = null;
			distance[i] = Double.NaN;
		}
	}
	
	/*private static final void sortNull(Object[] object, int j) {
		int i = object.length;
		while(--i > j && object[i] == null); //found last not null
		if(j>i) return;
		object[j] = object[i];
		object[i] = null;
	}*/
	
	private static final void sortByDistance(Player[] players, double[] distances, Random random) {
		int j = players.length;
		if (j == 0 || distances.length == 0) return;
		
	}
	
	private static int[] unrepeatableRandom(Random random, int range, int count) {
	    int[] result = new int[count];
	    int i = range;
	    while(i > 0 && count > 0) {
	    	double chance = random.nextDouble();
	    	if (chance < ((double) count) / (double) i--) {
	            result[--count] = i;
	        }
	    }
	    return result;
	}
	
	/*
	 * Maximal 101/165
	 * Max separator count = 5
	 * Max arg = 96
	 */
	private static final void filterPosition(Player[] players, String[] args) {
		int i = args.length, j = players.length;
		if (i == 0 || j == 0) return;
		final double[] x = new double[j], y = new double[j], z = new double[j];
		while(--j > -1) {
			if(players[j] == null) continue;
			Location loc = players[j].getLocation();
			x[j] = loc.getX();
			y[j] = loc.getY();
			z[j] = loc.getZ();
		}
		while(--i > -1) {
			if(args[i] == null) continue;
			if(args[i].startsWith("x>=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value < x[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("x>", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value <= x[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("x<=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value > x[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("x<", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value >= x[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			
			
			if(args[i].startsWith("y>=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value < y[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("y>", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value <= y[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("y<=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value > y[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("y<", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value >= y[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			
			
			if(args[i].startsWith("z>=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value < z[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("z>", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value <= z[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("z<=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(3);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value > z[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("z<", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(2);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value >= z[j]) {
						players[j] = null;
					}
				}
				continue;
			}
		}
	}
	
	/*
	 * Maximal 37/165
	 * Max separator count = 1
	 * Max arg = 36
	 */
	private static final double[] filterDistance(Player[] players, String[] args, Location location) {
		int i = args.length, j = players.length;
		if (i == 0 || j == 0) return new double[0];
		final double[] distancesqr = new double[j];
		double sx = location.getX(), sy = location.getY(), sz = location.getZ();
		while(--j > -1) {
			if(players[j] == null) {
				distancesqr[j] = Double.NaN;
				continue;
			}
			Location loc = players[j].getLocation();
			double x = sx - loc.getX(), y = sy - loc.getY(), z = sz - loc.getZ();
			x*=x;
			y*=y;
			z*=z;
			distancesqr[j] = x;
			distancesqr[j] += y;
			distancesqr[j] += z;
		}
		
		while(--i > -1) {
			if(args[i] == null) continue;
			if(args[i].startsWith("dist>=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(6);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				value*=value;
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value < distancesqr[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("dist<=", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(6);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				value*=value;
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value > distancesqr[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("dist>", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(5);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				value*=value;
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value <= distancesqr[j]) {
						players[j] = null;
					}
				}
				continue;
			}
			if(args[i].startsWith("dist<", 0)) {
				args[i] = null;
				String valuestr = args[i].substring(5);
				if(valuestr.isEmpty()) continue;
				double value;
				try {
					value = Double.parseDouble(valuestr);
				} catch (NumberFormatException e) {
					continue;
				}
				value*=value;
				j = players.length;
				while(--j > -1) {
					if(players[j] == null) continue;
					if(value >= distancesqr[j]) {
						players[j] = null;
					}
				}
				continue;
			}
		}
		return distancesqr;
	}
	
	/*
	 * Total argument value recomended not be bigger than 165
	 */
	private static final String[] getSelectorArgs(String selectorarg) {
		char[] chars = selectorarg.toCharArray();
		int i = chars.length;
		if(i < 3 || i > 2147483639 || chars[0] != '[' || chars[i-1] != ']') {
			return null;
		}
		int j = 1;
		while(--i > -1) {
			if(chars[i] == ',') {
				++j;
			}
		}
		int[] arglength = new int[j];
		i = chars.length;
		--i;
		int k = 0;
		while(--i > 0) {
			if(chars[i] == ',') {
				arglength[--j] = k;
				k = 0;
				continue;
			}
			++k;
		}
		arglength[0] = k;
		j = arglength.length;
		char[][] splitchar = new char[j][]; 
		i = chars.length;
		--i;
		while(--j>-1) {
			splitchar[j] = new char[arglength[j]];
			k = arglength[j];
			while(--i > 0 && --k > -1) {
				splitchar[j][k] = chars[i];
			}
		}
		j = splitchar.length;
		String[] args = new String[j];
		while(--j>-1) {
			args[j] = new String(splitchar[j]);
		}
		return args;
	}
	
	private final Location getSenderLocation(CommandSender sender) {
		if (sender instanceof BlockCommandSender) {
			BlockCommandSender commandblocksender = (BlockCommandSender) sender;
			return commandblocksender.getBlock().getLocation();
		} else if (sender instanceof CommandMinecart) {
			CommandMinecart commandminecartsender = (CommandMinecart) sender;
			return commandminecartsender.getLocation();
		} else if (sender instanceof Player) {
			Player playersender = (Player) sender;
			return playersender.getLocation();
		}
		return null;
	}

}
