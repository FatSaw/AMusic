package me.bomb.amusic;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.bomb.amusic.util.SimpleConfiguration;

final class ExecutorConfiguration {
	protected final String errors;
	
	private final boolean linkedqueue;
	private final int queuesize;
	private final int corePoolSize;
	private final int maximumPoolSize;
	private final TimeUnit timeUnit;
	private final long keepAlive;
	
	protected ExecutorConfiguration(final SimpleConfiguration sc, String parentsection) {
		final StringBuilder errors = new StringBuilder();
		final String queuetype = sc.getStringOrError(parentsection.concat("\0queue\0type"), errors);
		final int queuesize = sc.getIntOrError(parentsection.concat("\0queue\0size"), errors);
		final int corePoolSize = sc.getIntOrError(parentsection.concat("\0poolsize\0core"), errors);
		final int maximumPoolSize = sc.getIntOrError(parentsection.concat("\0poolsize\0maximum"), errors);
		final String keepAliveTimeUnit = sc.getStringOrError(parentsection.concat("\0keepalive\0timeunit"), errors);
		final long keepAlive = sc.getLongOrError(parentsection.concat("\0keepalive\0value"), errors);
		TimeUnit timeUnit = null;
		final boolean linkedqueue;
		if(queuetype.equals("LINKED")) {
			linkedqueue = true;
		} else if(queuetype.equals("ARRAY")) {
			linkedqueue = false;
		} else {
			linkedqueue = false;
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
		this.linkedqueue = linkedqueue;
		this.queuesize = queuesize;
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.keepAlive = keepAlive;
		this.timeUnit = timeUnit;
		this.errors = errors.toString();
	}
	
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
		TimeUnit timeUnit = null;
		final boolean linkedqueue;
		if(queuetype.equals("LINKED")) {
			linkedqueue = true;
		} else if(queuetype.equals("ARRAY")) {
			linkedqueue = false;
		} else {
			linkedqueue = false;
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
		this.linkedqueue = linkedqueue;
		this.queuesize = queuesize;
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.keepAlive = keepAlive;
		this.timeUnit = timeUnit;
		this.errors = errors.toString();
	}
	
	public Executor createExecutor() {
		if(errors.length() != 0) {
			return null;
		}
		Executor executor;
		try {
			executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive, timeUnit, this.linkedqueue ? new LinkedBlockingQueue<>(queuesize) : new ArrayBlockingQueue<>(queuesize, false));
		} catch (IllegalArgumentException | NullPointerException e) {
			executor = null;
		}
		return executor;
	}
	
	public Executor createExecutor(ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		if(errors.length() != 0) {
			return null;
		}
		Executor executor;
		try {
			executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive, timeUnit, this.linkedqueue ? new LinkedBlockingQueue<>(queuesize) : new ArrayBlockingQueue<>(queuesize, false), threadFactory, handler);
		} catch (IllegalArgumentException | NullPointerException e) {
			executor = null;
		}
		return executor;
	}
	
	private static void appendError(String error, StringBuilder sb) {
		sb.append(error);
		sb.append('\n');
	}
}