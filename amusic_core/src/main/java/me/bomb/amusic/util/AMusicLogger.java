package me.bomb.amusic.util;

public final class AMusicLogger {
	
	public AMusicLogger() {
	}
	
	private Logger logger;
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void info(String msg) {
		Logger logger = this.logger;
		if(logger == null) {
			return;
		}
		logger.info(msg);
	}
	
	public void warn(String msg) {
		Logger logger = this.logger;
		if(logger == null) {
			return;
		}
		logger.warn(msg);
	}
	
	public void error(String msg) {
		Logger logger = this.logger;
		if(logger == null) {
			return;
		}
		logger.error(msg);
	}

}
