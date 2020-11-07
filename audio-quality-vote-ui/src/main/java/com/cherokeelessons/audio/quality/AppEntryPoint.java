package com.cherokeelessons.audio.quality;

import com.cherokeelessons.audio.quality.dagger.AppComponents;
import com.cherokeelessons.audio.quality.dagger.DaggerAppComponents;
import com.cherokeelessons.audio.quality.presenter.AppPresenter;
import com.google.gwt.core.client.EntryPoint;

public class AppEntryPoint implements EntryPoint {

	@Override
	public void onModuleLoad() {
		final AppComponents appComponents = DaggerAppComponents.create();
		final AppPresenter appPresenter = appComponents.provideAppPresenter();
		appPresenter.init();
	}

}
