package com.cherokeelessons.audio.quality.model;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.fusesource.restygwt.client.REST;

import com.cherokeelessons.audio.quality.presenter.RunAsync;
import com.cherokeelessons.audio.quality.shared.UserInfo;

public class Api {
	@Inject
	protected RunAsync runasync;
 
	@Inject
	protected ClientSessionState state;
	
	@Inject
	protected RestApi rest;
	
	@Inject
	public Api() {
		//
	}
	
	public CompletableFuture<UserInfo> login(String idToken) {
		CallbackFuture<UserInfo> cf = new CallbackFuture<>();
		if (idToken==null) {
			runasync.run(()->cf.future().complete(null));
			return cf.future();
		}
		call(cf).login(idToken);
		return cf.future();
	}

	private String sessionId() {
		return state.sessionId()==null?"":state.sessionId();
	}

	private <T> RestApi call(CallbackFuture<T> cf) {
		return REST.withCallback(cf.callback()).call(rest);
	}
	
}
