package com.cherokeelessons.audio.quality.presenter;

import java.awt.Container;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.fusesource.restygwt.client.ServiceRoots;

import com.cherokeelessons.audio.quality.dagger.UiComponents;
import com.cherokeelessons.audio.quality.model.Api;
import com.cherokeelessons.audio.quality.model.ClientSessionState;
import com.cherokeelessons.audio.quality.model.Display;
import com.cherokeelessons.audio.quality.shared.Consts;
import com.cherokeelessons.audio.quality.shared.RestApi;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.cherokeelessons.audio.quality.ui.Loading;
import com.cherokeelessons.audio.quality.ui.Login;
import com.cherokeelessons.audio.quality.ui.MainMenu;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;

import elemental2.dom.DomGlobal;
import io.github.freddyboucher.gwt.oauth2.client.Auth;
import io.github.freddyboucher.gwt.oauth2.client.AuthRequest;

public class AppPresenter {

	@Inject
	protected Display display;

	@Inject
	protected Loading loading;

	@Inject
	protected ClientSessionState session;

	@Inject
	protected RunAsync async;

	@Inject
	protected Api api;

	@Inject
	protected UiComponents ui;

	@Inject
	public AppPresenter() {
		//
	}

	private String audioUrl(long vid) {
		return ServiceRoots.get("api")+RestApi.ApiPaths.audioFile.replace("{vid}", vid+"");
	}
	
	private String csvUrl() {
		return ServiceRoots.get("api")+RestApi.ApiPaths.audioQualityVotesCsv;
	}
	
	public void init() {
		ServiceRoots.add("api", GWT.getHostPageBaseURL()+"api");
		loading.loading(true, "Init");
		showLogin();
	}

	private void showLogin() {
		Login view = ui.loginUi();
		view.addAttachHandler((e) -> loading.loading(false));
		view.lnkLogin((n) -> {
			AuthRequest req = new AuthRequest("https", "accounts.google.com", "o/oauth2/auth", Consts.CLIENT_ID)
					.setParameter("scope", "email profile")
					.setParameter("response_type", "token id_token");
			Auth.get().login(req, new Callback<Map<String, String>, Throwable>() {
				@Override
				public void onFailure(Throwable reason) {
					GWT.log(null, reason);
				}

				@Override
				public void onSuccess(Map<String, String> result) {
					loading.loading(true);
					api.login(result.get("id_token")).thenAccept((u)->{
						loading.loading(false);
						if (u==null) {
							DomGlobal.console.log("token verification failed");
							return;
						}
						showMain();
					}).exceptionally(e->{
						loading.loading(false);
						DomGlobal.console.log(e.getMessage());
						return null;
					});
				}
			}, "access_token");
		});
		display.replace(view);
	}

	private void showMain() {
		MainMenu view = ui.mainMenuUi();
		display.replace(view);
		
		view.votesSubmitted((dataList)->{
			loading.loading(true);
			api.vote(dataList) //
				.thenRun(()->loading.loading(false)) //
				.thenRun(()->getAudio(view));
		});
		
		view.lnkVote((v)->getAudio(view));
		view.lnkLogout((v)->logout());
		view.lnkDownload((v)->download());
		view.lnkAbout((v)->view.showAbout());
		view.lnkStats((v)->showVoteStats(view));
		//show about page on first load
		view.showAbout();
	}

	private void showVoteStats(MainMenu view) {
		loading.loading(true);
		CompletableFuture<UserVoteCount> fMyVotes = api.myVotes();
		fMyVotes.thenAccept((myVotes)->{
			CompletableFuture<TopVoters> fTopVoters = api.topVoters();
			fTopVoters.thenAccept((topVoters)->{
				loading.loading(false);
				view.showStats(topVoters, myVotes);
			});
		});
	}

	private void download() {
		DomGlobal.window.open(csvUrl());
	}

	private void logout() {
		loading.loading(true);
		api.logout().thenAccept((v)->{
			session.clear();
			DomGlobal.location.reload(true);
		});
	}

	private void getAudio(MainMenu view) {
		loading.loading(true);
		api.pendingAudio().thenAccept(list-> {
			list.forEach((item)->item.setUrl(audioUrl(item.getVid())));
			view.setAudioDataList(list);
			loading.loading(false);
		});
		return;
	}
}
