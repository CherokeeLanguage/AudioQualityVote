package com.cherokeelessons.audio.quality.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import com.cherokeelessons.audio.quality.shared.AudioData;

public class AudioQualityVoteFiles {
	
	public static void main(String[] args) {
		getAudioData().forEach(System.out::println);
	}
	
	public static File getFolder() {
		return State.getAudioFolder();
	}
	
	public static List<AudioData> getAudioData() {
		List<AudioData> files = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(State.getAudioFolder().toPath())) {
			stream.forEach(p->{
				String filename = p.getFileName().toString();
				if (!filename.endsWith(".mp3")) {
					return;
				}
				Path t = p.resolveSibling(filename.substring(0, filename.length()-4)+".txt");
				if (!t.toFile().exists()) {
					return;
				}
				String text;
				try {
					text = Files.readString(t, StandardCharsets.UTF_8);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				files.add(new AudioData(p.toFile().getAbsolutePath(), text));	
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Collections.sort(files, (a,b)->a.getFile().compareToIgnoreCase(b.getFile()));
		return files;
	}
	
	private static class State {
		public static File getAudioFolder() {
			if (audioFolder == null) {
				loadPropertiesFile();
			}
			return audioFolder.getAbsoluteFile();
		}

		private static Properties properties;
		private static File audioFolder;

		private static void loadPropertiesFile() {
			if (properties != null) {
				return;
			}
			Properties props = new Properties();
			File file = AppPathConfig.PROPERTIES_FILE;
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
