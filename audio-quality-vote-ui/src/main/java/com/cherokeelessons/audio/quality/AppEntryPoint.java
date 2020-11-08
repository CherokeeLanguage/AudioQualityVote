package com.cherokeelessons.audio.quality;

import java.util.Map;

import com.cherokeelessons.audio.quality.dagger.AppComponents;
import com.cherokeelessons.audio.quality.dagger.DaggerAppComponents;
import com.cherokeelessons.audio.quality.presenter.AppPresenter;
import com.cherokeelessons.audio.quality.ui.LoadingView;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;

import io.github.freddyboucher.gwt.oauth2.client.Auth;
import io.github.freddyboucher.gwt.oauth2.client.AuthRequest;

public class AppEntryPoint implements EntryPoint {

	@Override
	public void onModuleLoad() {
		final AppComponents appComponents = DaggerAppComponents.create();
		final AppPresenter appPresenter = appComponents.provideAppPresenter();
//		appPresenter.init();
		GWT.log(GWT.getHostPageBaseURL());
		Button button = new Button("Google", (ClickHandler) event -> {
		      AuthRequest req =
		          new AuthRequest("https", "accounts.google.com", "o/oauth2/auth", "253590407731-1c84s03nf6qr9lfh7r8p0a4r6fo3tp4o.apps.googleusercontent.com")
		              .setParameter("scope", "email profile openid"); //
		              //.setParameter("redirect_uri", GWT.getHostPageBaseURL());
		      Auth.get().login(req, new Callback<Map<String, String>, Throwable>() {
		        @Override
		        public void onFailure(Throwable reason) {
		          GWT.log(null, reason);
		        }

		        @Override
		        public void onSuccess(Map<String, String> result) {
		          String token = result.get("access_token");
		          GWT.log(token);
		          GWT.log(result.toString());
		        }
		      }, "access_token");
		    });
		    RootPanel.get().add(button);
		    new LoadingView().loading(false);
	}

}
