package com.cherokeelessons.audio.quality.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.html.Span;

public class VoteStatsRow extends Composite {

	private static StatsDisplayRowUiBinder uiBinder = GWT.create(StatsDisplayRowUiBinder.class);

	interface StatsDisplayRowUiBinder extends UiBinder<Widget, VoteStatsRow> {
	}

	public VoteStatsRow() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@UiField
	protected Span uid;
	
	public void setUid(Long uid) {
		if (uid==null) {
			this.uid.clear();
			return;
		}
		NumberFormat fmt = NumberFormat.getFormat("#,##0;(#,##0)");
		this.uid.setText(fmt.format(uid));
	}
	
	@UiField
	protected Span votesCast;

	public void setVotesCast(int votesCast) {
		NumberFormat fmt = NumberFormat.getFormat("#,##0;(#,##0)");
		this.votesCast.setText(fmt.format(votesCast));
	}
}
