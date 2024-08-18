package me.bomb.amusic.converter;

final class FFmpegKiller extends Thread {
	
	private static int threadNumber;
    private static synchronized int nextThreadNum() {
        return threadNumber++;
    }
	
	private final Process process;

	protected FFmpegKiller(Process process) {
		super("FFmpegKiller-" + nextThreadNum());
		this.process = process;
	}

	@Override
	public void run() {
		process.destroy();
	}
}
