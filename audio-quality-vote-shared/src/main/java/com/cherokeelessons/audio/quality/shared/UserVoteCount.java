package com.cherokeelessons.audio.quality.shared;

public class UserVoteCount {
	private Long uid;
	private int pending;
	private int voted;
	public int getPending() {
		return pending;
	}
	public void setPending(int pending) {
		this.pending = pending;
	}
	public int getVoted() {
		return voted;
	}
	public void setVoted(int voted) {
		this.voted = voted;
	}
	public int getTotal() {
		return pending+voted;
	}
	public float getPercentVoted() {
		return ((float)getVoted())/((float)getTotal());
	}
	public float getPercentPending() {
		return 1f-getPercentVoted();
	}
	public Long getUid() {
		return uid;
	}
	public void setUid(Long uid) {
		this.uid = uid;
	}
}
