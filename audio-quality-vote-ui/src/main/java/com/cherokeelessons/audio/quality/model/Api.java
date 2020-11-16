package com.cherokeelessons.audio.quality.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.fusesource.restygwt.client.REST;

import com.cherokeelessons.audio.quality.presenter.RunAsync;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;

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
	
	public CompletableFuture<TopVoters> topVoters() {
		CallbackFuture<TopVoters> cf = new CallbackFuture<>();
		call(cf).topVoters(state.uid(), state.sessionId());
		return cf.future();
	}
	
	public CompletableFuture<UserVoteCount> myVotes() {
		CallbackFuture<UserVoteCount> cf = new CallbackFuture<>();
		call(cf).myVoteCounts(state.uid(), state.sessionId());
		return cf.future();
	}
	
	public CompletableFuture<AudioData> vote(AudioData data) {
		CallbackFuture<AudioData> cf = new CallbackFuture<>();
		call(cf).audioVote(state.uid(), state.sessionId(), data.getVid(), data.getBad(), data.getPoor(), data.getGood());
		return cf.future();
	}
	
	public CompletableFuture<Void> vote(List<AudioData> list) {
		AtomicInteger counter = new AtomicInteger(list.size());
		CompletableFuture<Void> cfv = new CompletableFuture<>();
		for (AudioData data: list) {
			CallbackFuture<AudioData> cf = new CallbackFuture<>();
			call(cf).audioVote(state.uid(), state.sessionId(), data.getVid(), data.getBad(), data.getPoor(), data.getGood());
			cf.future().thenRun(()->counter.decrementAndGet()).thenRun(()->{
				if (counter.get()>0) return;
				cfv.complete(null);
			});
		}
		return cfv;
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
		call(cf).audioListUndecided(state.uid(), state.sessionId(), 5);
		return cf.future();
	}

	private <T> RestApi call(CallbackFuture<T> cf) {
		return REST.withCallback(cf.callback()).call(rest);
	}

	public CompletableFuture<Void> logout() {
		CallbackFuture<Void> cf = new CallbackFuture<>();
		call(cf).logout(state.uid(), state.sessionId());
		return cf.future().exceptionally((e)->{
			state.clearOauth();
			return null;
		});
	}

	public CompletableFuture<Void> deleteSelf() {
		CallbackFuture<Void> cf = new CallbackFuture<>();
		call(cf).deleteSelf(state.uid(), state.sessionId());
		return cf.future().exceptionally((e)->{
			state.clear();
			return null;
		});
	}
	
}
