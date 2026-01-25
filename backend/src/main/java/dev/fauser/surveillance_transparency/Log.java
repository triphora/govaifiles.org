package dev.fauser.surveillance_transparency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Log {
	private static final Logger logger = LoggerFactory.getLogger("surveillance-transparency-app");

	public static Logger log() {
		return logger;
	}

	public static void info(String input) {
		logger.info(input);
	}

	public static void warn(String input) {
		logger.warn(input);
	}
}
