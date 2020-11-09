package com.cherokeelessons.audio.quality.dagger;

import javax.inject.Singleton;

import com.cherokeelessons.audio.quality.ui.Login;
import com.cherokeelessons.audio.quality.ui.MainMenu;

import dagger.Component;

@Singleton
@Component(modules = {UiModules.class, AppModules.class, AppSingletonModules.class})
public interface UiComponents {
	Login loginUi();
	MainMenu mainMenuUi();
}
