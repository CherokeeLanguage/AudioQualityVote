package com.cherokeelessons.audio.quality.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TopVoters implements Iterable<UserVoteCount> {
	
	private List<UserVoteCount> topVoters=new ArrayList<>();

	public List<UserVoteCount> getTopVoters() {
		return topVoters;
	}

	public void setTopVoters(List<UserVoteCount> topVoters) {
		this.topVoters = topVoters;
	}

	@Override
	public Iterator<UserVoteCount> iterator() {
		return topVoters.iterator();
	}
}
