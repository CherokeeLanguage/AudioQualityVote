package com.cherokeelessons.audio.quality.model;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.fusesource.restygwt.client.REST;

import com.cherokeelessons.audio.quality.presenter.RunAsync;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
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
	
	public CompletableFuture<AudioData> vote(AudioData data) {
		CallbackFuture<AudioData> cf = new CallbackFuture<>();
		call(cf).audioVote(state.uid(), state.sessionId(), data.getVid(), data.getBad(), data.getPoor(), data.getGood());
		return cf.future();
	}
	
	public CompletableFuture<UserInfo> login(String idToken) {
		CallbackFuture<UserInfo> cf = new CallbackFuture<>();
		CompletableFuture<UserInfo> future = cf.future();
		if (idToken==null) {
			runasync.run(()->future.complete(null));
			return future;
		}
		call(cf).login(idToken);
		future.thenApply((info)->{
			state.sessionId(info.getSessionId());
			state.uid(info.getUid());
			return info;
		});
		return future;
	}
	
	public CompletableFuture<AudioDataList> pendingAudio() {
		CallbackFuture<AudioDataList> cf = new CallbackFuture<AudioDataList>();
		call(cf).audioList(state.uid(), state.sessionId(), 5);
		return cf.future();
	}

	private <T> RestApi call(CallbackFuture<T> cf) {
		return REST.withCallback(cf.callback()).call(rest);
	}
	
}
