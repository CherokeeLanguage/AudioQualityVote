package com.cherokeelessons.audio.quality.ui;

import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MainMenu extends Composite implements UiView {

	private static MainMenuUiBinder uiBinder = GWT.create(MainMenuUiBinder.class);

	interface MainMenuUiBinder extends UiBinder<Widget, MainMenu> {
	}

	@Inject
	public MainMenu() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	public String getWindowTitle() {
		return "Main Menu";
	}

}
