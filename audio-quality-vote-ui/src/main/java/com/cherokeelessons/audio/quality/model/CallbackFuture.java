package com.cherokeelessons.audio.quality.model;

import java.util.concurrent.CompletableFuture;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import elemental2.dom.DomGlobal;

public class CallbackFuture<T> {
	private final CompletableFuture<T> future;
	private final MethodCallback<T> callback;

	public CallbackFuture() {
		future = new CompletableFuture<>();
		callback = new MethodCallback<T>() {
			@Override
			public void onFailure(Method method, Throwable exception) {
				DomGlobal.console.log("EXCEPTION: "+exception.getMessage());
				if (method != null && method.getResponse() != null) {
					Exception e = new Exception(method.getResponse().getText(), exception);
					future.completeExceptionally(e);
				} else {
					future.completeExceptionally(exception);
				}
			}

			@Override
			public void onSuccess(Method method, T response) {
				future.complete(response);
			}
		};
	}

	public CompletableFuture<T> future() {
		return future;
	}

	public MethodCallback<T> callback() {
		return callback;
	}
}
