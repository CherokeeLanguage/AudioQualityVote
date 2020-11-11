package com.cherokeelessons.audio.quality.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AboutDisplay extends Composite {

	private static AboutDisplayUiBinder uiBinder = GWT.create(AboutDisplayUiBinder.class);

	interface AboutDisplayUiBinder extends UiBinder<Widget, AboutDisplay> {
	}

	public AboutDisplay() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
