package com.cherokeelessons.audio.quality.ui;

import java.util.HashSet;
import java.util.Set;

import com.cherokeelessons.audio.quality.model.Handler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.MaterialButton;

public class Settings extends Composite {
	
	@UiField
	protected MaterialButton btnDeleteAccount;
	public HandlerRegistration btnDeleteAccount(Handler<Void> handler) {
		HandlerRegistration registration = btnDeleteAccount.addClickHandler((e)->handler.handle(null));
		registrations.add(registration);
		return registration;
	}
	
	private Set<HandlerRegistration> registrations=new HashSet<>();
	@Override
	protected void onDetach() {
		super.onDetach();
		for (HandlerRegistration registration: registrations) {
			registration.removeHandler();
		}
	}

	private static SettingsUiBinder uiBinder = GWT.create(SettingsUiBinder.class);

	interface SettingsUiBinder extends UiBinder<Widget, Settings> {
	}

	public Settings() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
