package com.cherokeelessons.audio.quality.dagger;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {UiModules.class, AppModules.class})
public interface UiComponents {

}
