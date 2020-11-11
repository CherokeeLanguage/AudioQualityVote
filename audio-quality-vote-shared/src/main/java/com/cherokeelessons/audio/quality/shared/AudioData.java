package com.cherokeelessons.audio.quality.shared;

public class AudioData {
	private String url;
	private long vid;
	private int bad;
	private int poor;
	private int good;
	private String audioFile;
	private String text;
	public AudioData() {
	}
	public AudioData(String audioFile, String text) {
		this.audioFile = audioFile;
		this.text = text;
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
	public String getAudioFile() {
		return audioFile;
	}
	public void setAudioFile(String audioFile) {
		this.audioFile = audioFile;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public long getVid() {
		return vid;
	}
	public void setVid(long vid) {
		this.vid = vid;
	}
}