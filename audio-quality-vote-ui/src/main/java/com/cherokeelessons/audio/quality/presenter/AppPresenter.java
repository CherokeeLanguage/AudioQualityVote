package com.cherokeelessons.audio.quality.presenter;

import java.util.List;
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
import com.cherokeelessons.audio.quality.ui.RecordAudio;
import com.cherokeelessons.audio.quality.ui.Settings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
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
		maybeShowLogin();
	}

	private void maybeShowLogin() {
		api.loggedIn().thenAccept((b)->{
			if (b) {
				loading.loading(false);
				showMain();
			} else {
				loading.loading(false);
				showLogin();
			}
		});
	}

	private void showLogin() {
		final Map<String, List<String>> params = Window.Location.getParameterMap();
		final String code = firstParam(params, "code");
		final String state = firstParam(params, "state");
		if (code != null && state != null) {
			handleOAuthReturn(code, state);
			return;
		}
		displayLoginView();
	}

	private static final String OAUTH_STATE_KEY = "oauth2.state";

	private void handleOAuthReturn(String code, String state) {
		final Storage storage = Storage.getSessionStorageIfSupported();
		final String expected = storage == null ? null : storage.getItem(OAUTH_STATE_KEY);
		if (expected != null && expected.equals(state)) {
			if (storage != null) {
				storage.removeItem(OAUTH_STATE_KEY);
			}
			// URL hygiene: the one-time code must not linger in the address
			// bar, history, or any shared URL after the state match consumes
			// this return. One replace-state, before the RPC, serves both the
			// success and failure outcomes; the state-mismatch path (below)
			// deliberately keeps the URL for troubleshooting.
			DomGlobal.window.history.replaceState(null, null, cleanUrl());
			loading.loading(true);
			api.login(code).thenAccept((u) -> {
				loading.loading(false);
				if (u == null) {
					DomGlobal.console.log("authorization code exchange failed");
					displayLoginView();
					return;
				}
				showMain();
			}).exceptionally(e -> {
				loading.loading(false);
				DomGlobal.console.log(e.getMessage());
				displayLoginView();
				return null;
			});
			return;
		}
		// state mismatch or unknown state — possible CSRF: abort, offer login
		// again. The state is consumed on success, so a replayed code URL also
		// lands here and creates nothing.
		DomGlobal.console.log("state mismatch - login aborted");
		displayLoginView();
	}

	/**
	 * The host-page base URL with no query parameters — the clean callback
	 * address the browser lands on after OAuth code/state/iss params are
	 * scrubbed.
	 */
	private static String cleanUrl() {
		String base = GWT.getHostPageBaseURL();
		return base.endsWith("/") ? base : base + "/";
	}

	private void displayLoginView() {
		Login view = ui.loginUi();
		view.addAttachHandler((e) -> loading.loading(false));
		view.lnkLogin((n) -> {
			String stateValue = makeRandomState();
			Storage storage = Storage.getSessionStorageIfSupported();
			if (storage != null) {
				storage.setItem(OAUTH_STATE_KEY, stateValue);
			}
			// GitHub web application flow: authorization-code form with
			// client-side state verification on return. redirect_uri is this
			// deployment's own origin (host page base URL) — with multiple
			// registered callbacks, an omitted redirect_uri would route the
			// code to the app's default callback instead of the requesting
			// deployment.
			String url = "https://github.com/login/oauth/authorize" //
					+ "?client_id=" + Consts.CLIENT_ID //
					+ "&scope=" + URL.encode("user:email") //
					+ "&state=" + URL.encode(stateValue) //
					+ "&redirect_uri=" + URL.encode(GWT.getHostPageBaseURL());
			Window.Location.assign(url);
		});
		display.replace(view);
	}

	private static String firstParam(Map<String, List<String>> params, String name) {
		List<String> values = params.get(name);
		return values == null || values.isEmpty() ? null : values.get(0);
	}

	/**
	 * 128 bits of hex entropy for the anti-CSRF {@code state} parameter.
	 * (elemental2 1.1 exposes no Crypto surface; a CSPRNG upgrade is a noted
	 * follow-up.)
	 */
	private static String makeRandomState() {
		StringBuilder sb = new StringBuilder(32);
		for (int i = 0; i < 8; i++) {
			String chunk = Integer.toHexString((int) (Math.random() * 0x10000));
			for (int j = chunk.length(); j < 4; j++) {
				sb.append('0');
			}
			sb.append(chunk);
		}
		return sb.toString();
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
		RecordAudio recView = view.showRecordView();
		recView.onSubmit((v)->{
			api.putUserAudio(recView.audioBlob(), recView.getMimeType(), "Jo:g hale:hwisd. [04-chr] | Random dictionary entry(ies)\n");
		});
	}

	private void showSettings(MainMenu view) {
		Settings settingsView = view.showSettings();
		settingsView.btnDeleteAccount((v) -> {
			ConfirmDialogControl cdc = showConfirmDialog("DELETE ACCOUNT?",
					"Deleting your account will also delete your votes and recorded audio tracks. This action cannot be reversed!");
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
			list.forEach((item) -> {
				Long aid = item.getAid();
				String encoded = URL.encode(item.getTxt().replaceAll("\\s+", " ").trim());
				String url = audioUrl(aid) + "?" + encoded;
				item.setUrl(url);
			});
			view.setAudioDataList(list);
			loading.loading(false);
		});
		return;
	}
}
