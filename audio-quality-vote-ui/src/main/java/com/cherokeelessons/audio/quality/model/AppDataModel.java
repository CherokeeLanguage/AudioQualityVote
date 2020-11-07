package com.cherokeelessons.audio.quality.model;

import com.google.gwt.core.client.GWT;

public class AppDataModel {
	protected final RestApi api;

	public AppDataModel() {
		api = GWT.create(RestApi.class);
	}
}
