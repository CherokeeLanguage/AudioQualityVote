package com.cherokeelessons.audio.quality.presenter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.fusesource.restygwt.client.Defaults;
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
import com.cherokeelessons.audio.quality.ui.Settings;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

import elemental2.dom.DomGlobal;
import gwt.material.design.client.constants.HeadingSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.TextAlign;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialDialog;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.html.Heading;
import gwt.material.design.client.ui.html.Hr;
import gwt.material.design.client.ui.html.Text;
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

	private String audioUrl(long aid) {
		return ServiceRoots.get("api") + RestApi.ApiPaths.AUDIO_FILE.replace("{aid}", aid + "");
	}

	private String csvUrl() {
		return ServiceRoots.get("api") + RestApi.ApiPaths.VOTES_CSV;
	}

	public void init() {
		Defaults.setAddXHttpMethodOverrideHeader(false);
		Defaults.setRequestTimeout(30000);
		ServiceRoots.add("api", GWT.getHostPageBaseURL() + "api");
		loading.loading(true, "Init");
		showLogin();
	}

	private void showLogin() {
		Login view = ui.loginUi();
		view.addAttachHandler((e) -> loading.loading(false));
		view.lnkLogin((n) -> {
			AuthRequest req = new AuthRequest("https", "accounts.google.com", "o/oauth2/auth", Consts.CLIENT_ID)
					.setParameter("scope", "email profile").setParameter("response_type", "token id_token");
			Auth.get().login(req, new Callback<Map<String, String>, Throwable>() {
				@Override
				public void onFailure(Throwable reason) {
					GWT.log(null, reason);
				}

				@Override
				public void onSuccess(Map<String, String> result) {
					loading.loading(true);
					api.login(result.get("id_token")).thenAccept((u) -> {
						loading.loading(false);
						if (u == null) {
							DomGlobal.console.log("token verification failed");
							return;
						}
						showMain();
					}).exceptionally(e -> {
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

		view.votesSubmitted((dataList) -> {
			loading.loading(true);
			api.vote(dataList) //
					.thenRun(() -> loading.loading(false)) //
					.thenRun(() -> getAudio(view));
		});

		view.lnkVote((v) -> getAudio(view));
		view.lnkLogout((v) -> logout());
		view.lnkDownload((v) -> download());
		view.lnkAbout((v) -> view.showAbout());
		view.lnkStats((v) -> showVoteStats(view));
		view.lnkSettings((v) -> showSettings(view));
		// show about page on first load
		view.showAbout();
		view.lnkRecord((v)->showRecord(view));
	}

	private void showRecord(MainMenu view) {
		view.showRecordView();
	}

	private void showSettings(MainMenu view) {
		Settings settingsView = view.showSettings();
		settingsView.btnDeleteAccount((v) -> {
			ConfirmDialogControl cdc = showConfirmDialog("DELETE ACCOUNT?",
					"Deleting your account will also delete your votes. This action cannot be reversed!");
			CompletableFuture<Boolean> cf = cdc.cf;
			cf.thenAccept((d) -> {
				if (d) {
					loading.loading(true);
					api.deleteSelf().thenRun(() -> DomGlobal.location.reload(true));
					session.clearCredentials();
				}
				cdc.dialog.close();
			});
		});
	}

	static class ConfirmDialogControl {
		public MaterialDialog dialog;
		public CompletableFuture<Boolean> cf;
	}

	private ConfirmDialogControl showConfirmDialog(String title, String message) {
		MaterialDialog md = new MaterialDialog();
		md.setTextAlign(TextAlign.CENTER);
		Heading heading = new Heading(HeadingSize.H4);
		heading.setText(title);
		md.add(heading);

		Text text = new Text();
		text.setText(message);

		MaterialColumn messageColumn = new MaterialColumn();
		messageColumn.setTextAlign(TextAlign.CENTER);
		messageColumn.add(text);

		MaterialRow messageRow = new MaterialRow();
		messageRow.add(messageColumn);

		md.add(messageRow);
		md.add(new Hr());

		MaterialButton yes = new MaterialButton("YES", IconType.CHECK);
		MaterialColumn yesColumn = new MaterialColumn(12, 6, 4);
		yesColumn.add(yes);

		MaterialButton no = new MaterialButton("NO", IconType.CANCEL);
		MaterialColumn noColumn = new MaterialColumn(12, 6, 4);
		noColumn.add(no);

		MaterialRow btnRow = new MaterialRow();
		btnRow.add(yesColumn);
		btnRow.add(noColumn);

		md.add(btnRow);

		CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();

		RootPanel.get().add(md);
		md.open();

		yes.addClickHandler((e) -> cf.complete(true));
		no.addClickHandler((e) -> cf.complete(false));

		ConfirmDialogControl cdc = new ConfirmDialogControl();
		cdc.dialog = md;
		cdc.cf = cf;
		return cdc;
	}

	private void showVoteStats(MainMenu view) {
		loading.loading(true);
		CompletableFuture<UserVoteCount> fMyVotes = api.myVotes();
		fMyVotes.thenAccept((myVotes) -> {
			CompletableFuture<TopVoters> fTopVoters = api.topVoters();
			fTopVoters.thenAccept((topVoters) -> {
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
		api.logout().thenAccept((v) -> {
			session.clearCredentials();
			DomGlobal.location.reload(true);
		});
	}

	private void getAudio(MainMenu view) {
		loading.loading(true);
		api.pendingAudio().thenAccept(list -> {
			list.forEach((item) -> item.setUrl(audioUrl(item.getAid())));
			view.setAudioDataList(list);
			loading.loading(false);
		});
		return;
	}
}
