package com.cherokeelessons.audio.quality.ui;

import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.ui.MaterialContainer;

public class VoteStats extends Composite {

	private static StatsDisplayUiBinder uiBinder = GWT.create(StatsDisplayUiBinder.class);

	interface StatsDisplayUiBinder extends UiBinder<Widget, VoteStats> {
	}

	public VoteStats() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setStats(TopVoters topVoters, UserVoteCount myvotes) {
		myStats.clear();
		topStats.clear();
		
		VoteStatsRow myRow = new VoteStatsRow();
		myRow.setUid(myvotes.getUid());
		myRow.setVotesCast(myvotes.getVoted());
		myStats.add(myRow);
		
		for (UserVoteCount topVoter: topVoters.getTopVoters()) {
			VoteStatsRow topRow = new VoteStatsRow();
			topRow.setUid(topVoter.getUid());
			topRow.setVotesCast(topVoter.getVoted());
			topStats.add(topRow);
		}
	}

	@UiField
	protected MaterialContainer myStats;
	
	@UiField
	protected MaterialContainer topStats;
}
