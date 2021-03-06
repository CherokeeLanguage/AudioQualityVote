package com.cherokeelessons.audio.quality.ui;

import javax.inject.Inject;

import gwt.material.design.client.ui.MaterialLoader;

public class Loading {
	@Inject
	public Loading() {
	}
 
	public void loading(boolean visible) {
		MaterialLoader.loading(visible);
	}
 
	public void loading(boolean visible, String message) {
		MaterialLoader.loading(visible, message);
	}
 
	public void progress(boolean visible) {
		MaterialLoader.progress(visible);
	}
}
