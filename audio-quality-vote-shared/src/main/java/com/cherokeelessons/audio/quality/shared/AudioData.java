package com.cherokeelessons.audio.quality.shared;

public class AudioData {
	private String url;
	private Long aid;
	private Long vid;
	private int bad;
	private int poor;
	private int good;
	private String file;
	private String txt;
	public AudioData() {
	}
	public AudioData(String file, String text) {
		this.setFile(file);
		this.setTxt(text);
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
	public Long getAid() {
		return aid;
	}
	public void setAid(Long aid) {
		this.aid = aid;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getTxt() {
		return txt;
	}
	public void setTxt(String txt) {
		this.txt = txt;
	}
}