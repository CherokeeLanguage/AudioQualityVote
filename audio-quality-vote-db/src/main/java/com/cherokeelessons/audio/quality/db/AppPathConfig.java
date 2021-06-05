package com.cherokeelessons.audio.quality.db;

import java.io.File;

public abstract class AppPathConfig {
	public static String TABLE_PREFIX;
	
	public static void findConfigFile(String folder, String context) {
		
		TABLE_PREFIX = context.replaceAll("(?i)[^a-z0-9_]", "_");
		if (!TABLE_PREFIX.matches("(?i)[a-z_].*")){
			TABLE_PREFIX = "_" + TABLE_PREFIX;
		}

		File parentFolder = new File(folder);
		File[] locations = {
				new File(parentFolder, "../" + context + ".properties"),
				new File(parentFolder, "../../"+context+".properties"),
				new File(parentFolder, "../"+context+".properties"),
				new File(parentFolder, "../../"+context+".properties"),
				new File(parentFolder, "../../../"+context+".properties"),
				new File(parentFolder, "../../../../"+context+".properties"),
				new File("/home/"+context+"/" + context + ".properties"),
				new File("/var/tmp/" + context + ".properties"),
				new File("/tmp/"+ context + ".properties"),
				new File(context + ".properties").getAbsoluteFile(),
				new File("../"+context+".properties"),
				new File("../../"+context+".properties"),
				new File("../../../"+context+".properties"),
				new File("../../../../"+context+".properties"),
		};
		for (File maybe: locations) {
			if (maybe.exists() && maybe.canRead()) {
				PROPERTIES_FILE = maybe.getAbsoluteFile();
				System.out.println("Properties file found: "+PROPERTIES_FILE.getAbsolutePath());
				break;
			}
			System.out.println("Properties file not found: "+maybe.getAbsolutePath());
		}
	}
	
	public static File PROPERTIES_FILE=new File("AudioQualityVote_db.properties").getAbsoluteFile();
}
