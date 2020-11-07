package com.cherokeelessons.audio.quality.dagger;

import javax.inject.Singleton;

import com.cherokeelessons.audio.quality.presenter.AppPresenter;
import com.cherokeelessons.audio.quality.presenter.RunAsync;

import dagger.Component;

@Singleton
@Component(modules = {AppModules.class, AppSingletonModules.class, UiModules.class})
public interface AppComponents {
	AppPresenter provideAppPresenter();
	RunAsync provideRunAsync();
}
