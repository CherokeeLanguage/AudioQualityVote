package com.cherokeelessons.audio.quality.ui;

import javax.inject.Inject;

import com.cherokeelessons.audio.quality.model.Handler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.html.Text;

public class Login extends Composite implements UiView {

	private static LoginUiBinder uiBinder = GWT.create(LoginUiBinder.class);

	interface LoginUiBinder extends UiBinder<Widget, Login> {
	}

	@Inject
	public Login() {
		initWidget(uiBinder.createAndBindUi(this));
		String display = BuildInfo.displayText();
		if (!display.isEmpty()) {
			buildInfo.setText(display);
		}
	}

	@UiField
	protected MaterialButton btnLogin;

	@UiField
	protected Text buildInfo;
	
	public HandlerRegistration lnkLogin(Handler<Void> handler) {
		return btnLogin.addClickHandler((evt)->handler.handle(null));
	}

	@Override
	public String getWindowTitle() {
		return "Login";
	}
}
