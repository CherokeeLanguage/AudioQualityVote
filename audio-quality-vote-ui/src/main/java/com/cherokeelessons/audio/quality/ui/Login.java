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

public class Login extends Composite implements UiView {

	private static LoginUiBinder uiBinder = GWT.create(LoginUiBinder.class);

	interface LoginUiBinder extends UiBinder<Widget, Login> {
	}

	@Inject
	public Login() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@UiField
	protected MaterialButton btnLogin;
	
	public HandlerRegistration lnkLogin(Handler<Void> handler) {
		return btnLogin.addClickHandler((evt)->handler.handle(null));
	}

	@Override
	public String getWindowTitle() {
		return "Login";
	}
}
