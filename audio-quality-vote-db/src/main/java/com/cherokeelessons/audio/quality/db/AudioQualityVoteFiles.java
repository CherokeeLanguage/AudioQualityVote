package com.cherokeelessons.audio.quality.db;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
					text = new String(Files.readAllBytes(t), StandardCharsets.UTF_8);
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
				// Environment-only configuration (AudioQualityVote#25): the
				// deployment guarantees AUDIO_FOLDER names an existing
				// readable directory. A missing value keeps the historical
				// behavior — the ingest scan fails per cycle and is
				// swallowed but logged by its caller.
				audioFolder = new File(System.getenv("AUDIO_FOLDER")).getAbsoluteFile();
			}
			return audioFolder;
		}

		private static File audioFolder;
	}
}
