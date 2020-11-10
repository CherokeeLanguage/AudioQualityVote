package com.cherokeelessons.audio.quality.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class AudioQualityVoteFiles {
	
	public static List<File> getFiles() {
		List<File> files = new ArrayList<File>();
		try (Stream<Path> stream = Files.list(State.audioFolder.toPath())) {
			stream.forEach(p->files.add(p.toFile()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		files.removeIf(f->!f.getName().toLowerCase().endsWith(".mp3"));
		return files;
	}
	
	protected static class State {
		public static File getAudioFolder() {
			if (audioFolder == null) {
				loadPropertiesFile();
			}
			return audioFolder;
		}

		protected static Properties properties;
		protected static File audioFolder;

		protected static void loadPropertiesFile() {
			if (properties != null) {
				return;
			}
			Properties props = new Properties();
			File file = new File(Consts.DEFAULT_PROPERTIES_FILE);
			if (!file.exists()) {
				file = new File(Consts.ALT_PROPERTIES_FILE);
			}
			try (FileInputStream in = new FileInputStream(file.getAbsoluteFile())) {
				props.load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Can't read " + file.getAbsolutePath(), e);
			}
			properties = props;
			audioFolder = new File(properties.getProperty("audio.folder"));
		}
	}
}
