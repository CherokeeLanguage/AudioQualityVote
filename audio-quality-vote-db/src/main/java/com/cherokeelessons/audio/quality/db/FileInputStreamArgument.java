package com.cherokeelessons.audio.quality.db;

import java.io.FileInputStream;

import org.jdbi.v3.core.argument.InputStreamArgument;

public class FileInputStreamArgument extends InputStreamArgument {
	public FileInputStreamArgument(FileInputStream stream, int length, boolean ascii) {
		super(stream, length, ascii);
	}
}
