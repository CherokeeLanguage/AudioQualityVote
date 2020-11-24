package com.cherokeelessons.audio.quality.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cherokeelessons.audio.quality.js.URL;
import com.cherokeelessons.audio.quality.model.Handler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

	protected Set<HandlerRegistration> submitRegistrations = new HashSet<>();

	public HandlerRegistration onSubmit(Handler<Void> handler) {
		HandlerRegistration handlerRegistration = new HandlerRegistration() {
			@Override
			public void removeHandler() {
				submitRegistrations.remove(this);
			}
		};
		submitRegistrations.add(handlerRegistration);
		return handlerRegistration;
	}
	
	protected Set<HandlerRegistration> onerrorRegistrations = new HashSet<>();

	public HandlerRegistration onError(Handler<String> handler) {
		HandlerRegistration handlerRegistration = new HandlerRegistration() {
			@Override
			public void removeHandler() {
				onerrorRegistrations.remove(this);
			}
		};
		onerrorRegistrations.add(handlerRegistration);
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
	
	private String src;
	protected MediaRecorder recorder;
	@Override
	protected void onLoad() {
		super.onLoad();
		List<ConstructorBlobPartsArrayUnionType> parts = new ArrayList<>();
		MediaStreamConstraints constraints = MediaStreamConstraints.create();
		constraints.setAudio(true);
		Promise<MediaStream> userMedia = DomGlobal.navigator.mediaDevices.getUserMedia(constraints);
		userMedia.catch_((c)->{
			
			return null;
		});
		userMedia.then(media -> {
			MediaRecorderOptions options = MediaRecorderOptions.create();
			options.setBitsPerSecond(44100);
			//options.setMimeType("audio/*");
			recorder = new MediaRecorder(media, options);
			recorder.ondataavailable=(ev)->{
				GWT.log("recorder#ondataavailable");
				BlobEvent evb = Js.cast(ev);
				if (evb.data.size==0) {
					return media;
				}
				parts.add(ConstructorBlobPartsArrayUnionType.of(evb.data));
				return media;
			};
			recorder.onstop=(ev)->{
				GWT.log("recorder#onstop");
				btnRecordStart.setEnabled(true);
				btnRecordStop.setEnabled(false);
				BlobPropertyBag p = BlobPropertyBag.create();
				p.setType("audio/mp3");
				Blob mp3 = new Blob((ConstructorBlobPartsArrayUnionType[]) parts.toArray(), p);
				if (mp3.size>0) {
					revokeSrc();
					src = URL.createObjectURL(mp3);
					audio.setSrc(src);
					audio.setControls(true);
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
		});
	}
}
