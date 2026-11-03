package com.cherokeelessons.audio.quality.db;

public abstract class AppPathConfig {
	public static String TABLE_PREFIX;

	/**
	 * Derives the table prefix from the servlet context path. Configuration
	 * itself arrives solely through the process environment (see
	 * {@code AudioQualityVoteDao.State} and {@code AudioQualityVoteFiles});
	 * no configuration file is read.
	 */
	public static void setContext(String context) {
		TABLE_PREFIX = context.replaceAll("(?i)[^a-z0-9_]", "_");
		if (!TABLE_PREFIX.matches("(?i)[a-z_].*")){
			TABLE_PREFIX = "_" + TABLE_PREFIX;
		}
	}
}
