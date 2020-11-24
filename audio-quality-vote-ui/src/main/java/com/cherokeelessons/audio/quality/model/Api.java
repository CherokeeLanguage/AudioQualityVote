package com.cherokeelessons.audio.quality.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.HeaderParam;

import org.fusesource.restygwt.client.REST;
import org.fusesource.restygwt.client.ServiceRoots;

import com.cherokeelessons.audio.quality.js.JSON;
import com.cherokeelessons.audio.quality.presenter.RunAsync;
import com.cherokeelessons.audio.quality.shared.AudioBytesInfo;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.github.nmorel.gwtjackson.client.exception.JsonDeserializationException;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;

import elemental2.dom.Blob;
import elemental2.dom.DomGlobal;
import elemental2.dom.ProgressEvent;
import elemental2.dom.XMLHttpRequest;
import jsinterop.base.Js;

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

	static interface AudioBytesInfoMapper extends ObjectMapper<AudioBytesInfo> {
	}

	public CompletableFuture<AudioBytesInfo> putUserAudio(Blob blob, String text) {
		return putUserAudio(blob, text, (e) -> {});
	}

	public CompletableFuture<AudioBytesInfo> putUserAudio(Blob blob, String text, //
			XMLHttpRequest.OnprogressFn onprogress
			) {
		CompletableFuture<AudioBytesInfo> cf = new CompletableFuture<>();
		if (blob == null) {
			runasync.run(()->cf.completeExceptionally(new NullPointerException("blob is null")));
			return cf;
		}
		if (onprogress == null) {
			runasync.run(()->cf.completeExceptionally(new NullPointerException("onprogress is null")));
			return cf;
		}
		AudioBytesInfoMapper mapper = GWT.create(AudioBytesInfoMapper.class);
		XMLHttpRequest xhr=new XMLHttpRequest();
		xhr.open("PUT", ServiceRoots.get("api")+RestApi.ApiPaths.AUDIO_PUT, true);
		xhr.onabort=(e)->cf.completeExceptionally(new RuntimeException(JSON.stringify(e)));
		xhr.onerror=(e)->cf.completeExceptionally(new RuntimeException(JSON.stringify(e)));
		xhr.onprogress=(e)->onprogress.onInvoke(e);
		xhr.onload = (e) -> {
			AudioBytesInfo info;
			try {
				info = mapper.read(xhr.responseText);
				cf.complete(info);
			} catch (Exception e1) {
				cf.completeExceptionally(e1);
			}			
		};
		xhr.setRequestHeader("uid", ""+state.uid());
		xhr.setRequestHeader("session-id", state.sessionId());
		xhr.send(blob);
		return cf;
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
		call(cf).audioVote(state.uid(), state.sessionId(), data.getVid(), data.getBad(), data.getPoor(),
				data.getGood());
		return cf.future();
	}

	public CompletableFuture<Void> vote(List<AudioData> list) {
		AtomicInteger counter = new AtomicInteger(list.size());
		CompletableFuture<Void> cfv = new CompletableFuture<>();
		for (AudioData data : list) {
			CallbackFuture<AudioData> cf = new CallbackFuture<>();
			call(cf).audioVote(state.uid(), state.sessionId(), data.getVid(), data.getBad(), data.getPoor(),
					data.getGood());
			cf.future().thenRun(() -> counter.decrementAndGet()).thenRun(() -> {
				if (counter.get() > 0)
					return;
				cfv.complete(null);
			});
		}
		return cfv;
	}

	public CompletableFuture<UserInfo> login(String idToken) {
		CallbackFuture<UserInfo> cf = new CallbackFuture<>();
		CompletableFuture<UserInfo> future = cf.future();
		if (idToken == null) {
			runasync.run(() -> future.complete(null));
			return future;
		}
		call(cf).login(idToken);
		future.thenApply((info) -> {
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
		return cf.future().exceptionally((e) -> {
			state.clearCredentials();
			return null;
		});
	}

	public CompletableFuture<Void> deleteSelf() {
		CallbackFuture<Void> cf = new CallbackFuture<>();
		call(cf).deleteSelf(state.uid(), state.sessionId());
		return cf.future().exceptionally((e) -> {
			state.clear();
			return null;
		});
	}

	public CompletableFuture<Boolean> loggedIn() {
		CallbackFuture<Boolean> cf = new CallbackFuture<>();
		if (state.uid()==null || state.uid()<1 || state.sessionId()==null || state.sessionId().trim().isEmpty()) {
			runasync.run(()->cf.future().complete(false));
			return cf.future();
		}
		call(cf).isSessionId(state.uid(), state.sessionId());
		return cf.future().exceptionally((e)->{
			return false;
		});
	}

}
