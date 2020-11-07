package com.cherokeelessons.audio.quality.dagger;

import java.util.HashMap;

import javax.inject.Singleton;

import org.fusesource.restygwt.client.Defaults;

import com.cherokeelessons.audio.quality.model.ClientSessionState;
import com.cherokeelessons.audio.quality.model.RestApi;
import com.google.gwt.core.client.GWT;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.storage.client.StorageMap;

import dagger.Module;
import dagger.Provides;

@Module
public abstract class AppSingletonModules {
	@Singleton
	@Provides
	static UiComponents uiComponents() {
		GWT.log("@Provides: UiComponents");
		return DaggerUiComponents.create();
	}
 
	@Singleton
	@Provides
	static ClientSessionState providesAppState() {
		GWT.log("@Provides: ClientSessionState");
		Storage storage = Storage.getLocalStorageIfSupported();
		if (storage==null) {
			return new ClientSessionState(new HashMap<String, String>());
		}
		return new ClientSessionState(new StorageMap(storage));
	}
	
	@Singleton
	@Provides
	static RestApi provideRestApi() {
		GWT.log("@Provides: DirectRestApi");
		final String serviceRoot = GWT.getHostPageBaseURL() + "api";
		GWT.log("Service Root: " + serviceRoot);
		Defaults.setServiceRoot(serviceRoot);
		return GWT.create(RestApi.class);
	}
}
