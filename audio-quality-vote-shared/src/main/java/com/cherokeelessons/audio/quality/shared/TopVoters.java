package com.cherokeelessons.audio.quality.shared;

import java.util.ArrayList;
import java.util.List;

public class TopVoters {
	private List<UserVoteCount> topVoters=new ArrayList<>();

	public List<UserVoteCount> getTopVoters() {
		return topVoters;
	}

	public void setTopVoters(List<UserVoteCount> topVoters) {
		this.topVoters = topVoters;
	}
}
