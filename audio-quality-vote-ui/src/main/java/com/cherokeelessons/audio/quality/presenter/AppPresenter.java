package com.cherokeelessons.audio.quality.presenter;

import javax.inject.Inject;

import com.cherokeelessons.audio.quality.model.Api;
import com.cherokeelessons.audio.quality.model.ClientSessionState;
import com.cherokeelessons.audio.quality.ui.LoadingView;

public class AppPresenter {
	@Inject
	protected LoadingView loading;
	
	@Inject
	protected ClientSessionState session;
	
	@Inject
	protected RunAsync async;
	
	@Inject
	protected Api api;
	
	@Inject
	public AppPresenter() {
		//
	}

	public void init() {
		loading.loading(true, "Init");
		api.login().exceptionally((e)->{
			return false;
		}).thenAccept((b)->{
			if (b) {
				showMain();
			} else {
				showLogin();
			}
		});
	}

	private void showLogin() {
		// TODO Auto-generated method stub
		
	}

	private void showMain() {
		// TODO Auto-generated method stub
		
	}
}
