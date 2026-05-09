package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import me.bomb.amusic.util.SimpleConfiguration;

final class ExecutorConfiguration {
	protected final String errors;
	protected final Executor executor;
	protected ExecutorConfiguration(String resource) {
		final StringBuilder errors = new StringBuilder();
		final SimpleConfiguration sc;
		{
			byte[] bytes = null;
			int size = 0;
			InputStream is = null;
			try {
				is = ExecutorConfiguration.class.getClassLoader().getResourceAsStream(resource);
				bytes = new byte[0x100];
				size = is.read(bytes, 0, bytes.length);
				is.close();
			} catch (IOException e3) {
				appendError("Filed to read executor config ".concat(resource), errors);
				if(is != null) {
					try {
						is.close();
					} catch (IOException e4) {
					}
				}
			}
			sc = new SimpleConfiguration(bytes, size);
		}
		final String queuetype = sc.getStringOrError("executor\0queue\0type", errors);
		final int queuesize = sc.getIntOrError("executor\0queue\0size", errors);
		final int corePoolSize = sc.getIntOrError("executor\0poolsize\0core", errors);
		final int maximumPoolSize = sc.getIntOrError("executor\0poolsize\0maximum", errors);
		final String keepAliveTimeUnit = sc.getStringOrError("executor\0keepalive\0timeunit", errors);
		final long keepAlive = sc.getLongOrError("executor\0keepalive\0value", errors);
		BlockingQueue<Runnable> queue = null;
		TimeUnit timeUnit = null;
		if(queuetype.equals("LINKED")) {
			queue = new LinkedBlockingQueue<>(queuesize);
		} else if(queuetype.equals("ARRAY")) {
			queue = new ArrayBlockingQueue<>(queuesize, false);
		} else {
			appendError("Unknown queue type", errors);
		}
		if(queuesize < 1) {
			appendError("Invalid queue size", errors);
		}
		if(corePoolSize < 0) {
			appendError("Invalid core pool size", errors);
		}
		if(maximumPoolSize < 1) {
			appendError("Invalid maximum pool size", errors);
		}
		if(keepAlive < 0) {
			appendError("Invalid keep alive", errors);
		}
		try {
			timeUnit = TimeUnit.valueOf(keepAliveTimeUnit);
		} catch (IllegalArgumentException e) {
			appendError("Unknown time unit type", errors);
		}
		if(errors.length() == 0) {
			Executor executor;
			try {
				executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive, timeUnit, queue);
			} catch (IllegalArgumentException | NullPointerException e) {
				appendError("Thread pool executor initialization filed", errors);
				executor = null;
			}
			this.executor = executor;
		} else {
			this.executor = null;
		}
		this.errors = errors.toString();
	}
	private static void appendError(String error, StringBuilder sb) {
		sb.append(error);
		sb.append('\n');
	}
}