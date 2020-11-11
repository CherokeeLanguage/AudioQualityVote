package com.cherokeelessons.audio.quality.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AudioDataList implements Iterable<AudioData> {
	private List<AudioData> list;
	
	public AudioDataList() {
		setList(new ArrayList<AudioData>());
	}

	public List<AudioData> getList() {
		return list;
	}

	public void setList(List<AudioData> list) {
		this.list = list;
	}

	@Override
	public Iterator<AudioData> iterator() {
		return list.iterator();
	}
}
