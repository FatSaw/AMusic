package me.bomb.amusic.velocity;

import java.io.File;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class Data extends me.bomb.amusic.Data {
	
	private final File datafile;
	
	protected Data(File datafile) {
		super();
		this.datafile = datafile;
	}

	@Override
	protected void save() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void load() {
		options.clear();
		final YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(datafile).build();
	    try {
			final CommentedConfigurationNode data = loader.load();
			//data.get
		} catch (ConfigurateException e) {
		}
	}

}
