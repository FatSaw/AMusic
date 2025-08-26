package me.bomb.amusic.util;

public final class AMusicLogger {
	
	private AMusicLogger() {
	}
	
	private static Logger logger;
	
	public static void setLogger(Logger logger) {
		AMusicLogger.logger = logger;
	}

	public static void info(String msg) {
		if(AMusicLogger.logger == null) {
			return;
		}
		AMusicLogger.logger.info(msg);
	}

	public static void warn(String msg) {
		if(AMusicLogger.logger == null) {
			return;
		}
		AMusicLogger.logger.warn(msg);
	}

	public static void error(String msg) {
		if(AMusicLogger.logger == null) {
			return;
		}
		AMusicLogger.logger.error(msg);
	}

}
