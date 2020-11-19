package com.cherokeelessons.audio.quality.ui;

import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.Document;

import gwt.material.design.client.base.MaterialWidget;

public class Audio extends MaterialWidget {
	protected AudioElement element;
	protected SourceAudio[] sources;
	protected boolean loop = false;
	protected boolean autoplay = false;
	protected boolean muted = false;
	protected boolean controls = true;

	protected String type = SourceAudio.Type.MPEG.name();
	protected String src;
	protected String alt;
	protected String preload = Preload.NONE.name();

	public boolean isControls() {
		return controls;
	}

	public void setControls(boolean controls) {
		this.controls = controls;
	}

	public String getPreload() {
		return preload;
	}

	public void setPreload(String preload) {
		this.preload = preload;
	}

	public static enum Preload {
		AUTO, METADATA, NONE;
	}

	public Audio() {
		super(Document.get().createAudioElement());
	}

	@Override
	protected void onLoad() {
		super.onLoad();

		element = getElement().cast();

		element.setAutoplay(autoplay);
		element.setControls(controls);
		element.setLoop(loop);
		element.setMuted(muted);
		element.setPreload(preload.toLowerCase());
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
		element.setLoop(loop);
	}

	public boolean isAutoplay() {
		return autoplay;
	}

	public void setAutoplay(boolean autoplay) {
		this.autoplay = autoplay;
		element.setAutoplay(autoplay);
	}

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
		element.setMuted(muted);
	}

	public SourceAudio[] getSources() {
		return sources;
	}

	public void setSources(SourceAudio[] sources) {
		if (this.sources != null && this.sources.length > 0) {
			for (SourceAudio source : this.sources) {
				element.removeChild(source.element);
			}
		}
		for (SourceAudio source : sources) {
			element.appendChild(source.element);
		}
		this.sources = sources;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		element.setAttribute("type", type);
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
		element.setSrc(src);
	}

	public String getAlt() {
		return alt;
	}

	public void setAlt(String alt) {
		this.alt = alt;
		element.setTitle(alt);
	}

}
