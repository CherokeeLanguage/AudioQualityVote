package com.cherokeelessons.audio.quality.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cherokeelessons.audio.quality.js.JSON;
import com.cherokeelessons.audio.quality.js.URL;
import com.cherokeelessons.audio.quality.model.Handler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import elemental2.dom.Blob;
import elemental2.dom.Blob.ConstructorBlobPartsArrayUnionType;
import elemental2.dom.BlobEvent;
import elemental2.dom.BlobPropertyBag;
import elemental2.dom.DomGlobal;
import elemental2.dom.MediaRecorder;
import elemental2.dom.MediaRecorderOptions;
import elemental2.dom.MediaStream;
import elemental2.dom.MediaStreamConstraints;
import elemental2.promise.Promise;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import jsinterop.base.Js;

public class RecordAudio extends Composite {

	private static RecordAudioUiBinder uiBinder = GWT.create(RecordAudioUiBinder.class);

	interface RecordAudioUiBinder extends UiBinder<Widget, RecordAudio> {
	}

	@UiField
	protected Audio audio;

	@UiField
	protected MaterialButton btnRecordStart;

	@UiField
	protected MaterialButton btnRecordStop;

	@UiField
	protected MaterialButton btnSubmit;
	public HandlerRegistration onSubmit(Handler<Void> handler) {
		return btnSubmit.addClickHandler((e)->handler.handle(null));
	}
	
	protected Set<Handler<String>> errorHandlers = new HashSet<>();

	public HandlerRegistration onError(final Handler<String> handler) {
		HandlerRegistration handlerRegistration = new HandlerRegistration() {
			@Override
			public void removeHandler() {
				errorHandlers.remove(handler);
			}
		};
		errorHandlers.add(handler);
		return handlerRegistration;
	}

	public RecordAudio() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		revokeSrc();
	}
	
	private void revokeSrc() {
		if (src!=null && src.startsWith("blob:")) {
			URL.revokeObjectURL(src);	
		}
	}
	
	@UiField
	protected MaterialLabel lblText;
	public void setText(String text) {
		lblText.setText(text);
	}
	
	@UiField
	protected MaterialLabel lblSyllabary;
	public void setSyllabary(String text) {
		lblSyllabary.setText(text);
	}
	
	public Blob audioBlob() {
		return audioBlob;
	}
	
	private Blob audioBlob;
	private String src;
	public String getSrc() {
		return src;
	}

	protected String mimeType = "";
	protected MediaRecorder recorder;
	@Override
	protected void onLoad() {
		super.onLoad();
		audio.setControls(false);
		List<ConstructorBlobPartsArrayUnionType> parts = new ArrayList<>();
		MediaStreamConstraints constraints = MediaStreamConstraints.create();
		constraints.setAudio(true);
		constraints.setVideo(false);
		Promise<MediaStream> userMedia = DomGlobal.navigator.mediaDevices.getUserMedia(constraints);
		userMedia.then(media -> {
			MediaRecorderOptions options = MediaRecorderOptions.create();
			recorder = new MediaRecorder(media, options);
			//mimeType = recorder.mimeType;
			recorder.ondataavailable=(ev)->{
				GWT.log("recorder#ondataavailable");
				BlobEvent evb = Js.cast(ev);
				if (evb.data.size==0) {
					return media;
				}
				mimeType=evb.data.type;
				parts.add(ConstructorBlobPartsArrayUnionType.of(evb.data));
				return media;
			};
			recorder.onstop=(ev)->{
				GWT.log("recorder#onstop");
				btnRecordStart.setEnabled(true);
				btnRecordStop.setEnabled(false);
				BlobPropertyBag p = BlobPropertyBag.create();
				p.setType(mimeType);
				audioBlob = new Blob((ConstructorBlobPartsArrayUnionType[]) parts.toArray(), p);
				audioBlob.type=mimeType;
				if (audioBlob.size>0) {
					revokeSrc();
					src = URL.createObjectURL(audioBlob);
					audio.setSrc(src);
					audio.setControls(true);
					audio.type=mimeType;
					btnSubmit.setEnabled(true);
				}
				return media;				
			};
			recorder.onstart=(ev)->{
				GWT.log("recorder#onstart");
				parts.clear();
				revokeSrc();
				btnRecordStart.setEnabled(false);
				btnRecordStop.setEnabled(true);
				audio.setControls(false);
				return media;
			};
			btnRecordStart.addClickHandler((e)->recorder.start());
			btnRecordStop.addClickHandler((e)->recorder.stop());
			btnRecordStop.setEnabled(false);
			btnSubmit.setEnabled(false);
			audio.setControls(false);
			return null;
		}).catch_((c)->{
			// NOTIFY PERMISSION DENIED OR NO INPUT!
			for (Handler<String> onError: errorHandlers) {
				new Timer() {
					@Override
					public void run() {
						onError.handle(JSON.stringify(c));
					}
				};
			}
			GWT.log("NO INPUT OR NO PERMISSION!");
			return null;
		});
	}

	public String getMimeType() {
		if (mimeType.contains(";")) {
			return mimeType.substring(0, mimeType.indexOf(";")).trim();
		}
		return mimeType;
	}
}
