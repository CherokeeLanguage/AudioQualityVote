package com.cherokeelessons.audio.quality.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AudioInfoList implements Iterable<AudioInfo> {
	private List<AudioInfo> list;
	
	public AudioInfoList() {
		setList(new ArrayList<AudioInfo>());
	}

	public List<AudioInfo> getList() {
		return list;
	}

	public void setList(List<AudioInfo> list) {
		this.list = list;
	}

	@Override
	public Iterator<AudioInfo> iterator() {
		return list.iterator();
	}
}
