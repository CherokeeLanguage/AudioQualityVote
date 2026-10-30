package com.cherokeelessons.audio.quality.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.html.Text;

public class AboutDisplay extends Composite {

	private static AboutDisplayUiBinder uiBinder = GWT.create(AboutDisplayUiBinder.class);

	interface AboutDisplayUiBinder extends UiBinder<Widget, AboutDisplay> {
	}

	@UiField
	protected Text buildInfo;

	public AboutDisplay() {
		initWidget(uiBinder.createAndBindUi(this));
		String display = BuildInfo.displayText();
		if (!display.isEmpty()) {
			buildInfo.setText(display);
		}
	}

}
