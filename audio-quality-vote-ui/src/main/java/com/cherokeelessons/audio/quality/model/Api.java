package com.cherokeelessons.audio.quality.model;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.fusesource.restygwt.client.REST;

import com.cherokeelessons.audio.quality.presenter.RunAsync;

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
	
	public CompletableFuture<Boolean> login() {
		CallbackFuture<Boolean> cf = new CallbackFuture<>();
		if (state.oauth()==null) {
			runasync.run(()->cf.future().complete(false));
			return cf.future();
		}
		call(cf).login(state.oauth());
		return cf.future();
	}

	private String sessionId() {
		return state.sessionId()==null?"":state.sessionId();
	}

	private <T> RestApi call(CallbackFuture<T> cf) {
		return REST.withCallback(cf.callback()).call(rest);
	}
	
}
