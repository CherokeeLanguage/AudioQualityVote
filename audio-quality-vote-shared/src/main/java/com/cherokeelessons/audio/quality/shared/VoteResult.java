package com.cherokeelessons.audio.quality.shared;

public class VoteResult {
	private String txt;
	private String file;
	private Long aid;
	private int bad;
	private int poor;
	private int good;
	private int votes;
	private float ranking;
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public int getBad() {
		return bad;
	}
	public void setBad(int bad) {
		this.bad = bad;
	}
	public int getPoor() {
		return poor;
	}
	public void setPoor(int poor) {
		this.poor = poor;
	}
	public int getGood() {
		return good;
	}
	public void setGood(int good) {
		this.good = good;
	}
	public float getRanking() {
		return ranking;
	}
	public void setRanking(float ranking) {
		this.ranking = ranking;
	}
	public int getVotes() {
		return votes;
	}
	public void setVotes(int votes) {
		this.votes = votes;
	}
	public Long getAid() {
		return aid;
	}
	public void setAid(Long aid) {
		this.aid = aid;
	}
	public String getTxt() {
		return txt;
	}
	public void setTxt(String txt) {
		this.txt = txt;
	}
}
