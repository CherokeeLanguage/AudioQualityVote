package com.cherokeelessons.audio.quality.presenter;

import javax.inject.Inject;

import com.google.gwt.user.client.Timer;

public class RunAsync {

	@Inject
	public RunAsync() {
		//
	}

	public void run(Runnable runnable, int delayMs) {
		new Timer() {
			@Override
			public void run() {
				runnable.run();
			}
		}.schedule(delayMs);
	}
	
	public void run(Runnable runnable) {
		run(runnable, 1);
	}
}
