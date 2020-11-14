package com.cherokeelessons.audio.quality.ui;

import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.MaterialButton;

public class ShareButtons extends Composite {
	
	private String href;

	private static ShareButtonsUiBinder uiBinder = GWT.create(ShareButtonsUiBinder.class);

	interface ShareButtonsUiBinder extends UiBinder<Widget, ShareButtons> {
	}

	@Inject
	public ShareButtons() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
		updateButtons();
	}

	private void updateButtons() {
		String facebook = "https://www.facebook.com/sharer.php?u={url}";
		String twitter = "https://twitter.com/intent/tweet?url={url}&text={text}&hashtags={hashtags}";
		String linkedin = "https://www.linkedin.com/shareArticle?mini=true&url={url}&title={title}&summary={text}";
	}

}
