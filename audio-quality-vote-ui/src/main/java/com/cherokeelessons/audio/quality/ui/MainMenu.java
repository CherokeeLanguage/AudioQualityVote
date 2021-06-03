package com.cherokeelessons.audio.quality.ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.cherokeelessons.audio.quality.model.Handler;
import com.cherokeelessons.audio.quality.presenter.RunAsync;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import gwt.material.design.client.constants.HeadingSize;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialRadioButton;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.html.Heading;
import gwt.material.design.client.ui.html.Hr;

public class MainMenu extends Composite implements UiView {

	private static MainMenuUiBinder uiBinder = GWT.create(MainMenuUiBinder.class);

	interface MainMenuUiBinder extends UiBinder<Widget, MainMenu> {
	}

	@Inject
	public MainMenu() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@Override
	public String getWindowTitle() {
		return "Main Menu";
	}
	
	@UiField
	protected MaterialLink lnkRecord;
	public HandlerRegistration lnkRecord(Handler<Void> handler) {
		lnkRecord.setEnabled(false);
		return lnkRecord.addClickHandler((e)->handler.handle(null));
	}
	
	@UiField
	protected MaterialLink lnkDownload;
	public HandlerRegistration lnkDownload(Handler<Void> handler) {
		return lnkDownload.addClickHandler((e)->handler.handle(null));
	}
	
	@UiField
	protected MaterialLink lnkSettings;
	public HandlerRegistration lnkSettings(Handler<Void> handler) {
		return lnkSettings.addClickHandler((e)->handler.handle(null));
	}

	@UiField
	protected MaterialLink lnkVote;
	public HandlerRegistration lnkVote(Handler<Void> handler) {
		return lnkVote.addClickHandler((e)->handler.handle(null));
	}
	
	@UiField
	protected MaterialLink lnkAbout;
	public HandlerRegistration lnkAbout(Handler<Void> handler) {
		return lnkAbout.addClickHandler((e)->handler.handle(null));
	}
	
	@UiField
	protected MaterialLink lnkLogout;
	public HandlerRegistration lnkLogout(Handler<Void> handler) {
		return lnkLogout.addClickHandler((e)->handler.handle(null));
	}

	@UiField
	protected MaterialLink lnkStats;
	public HandlerRegistration lnkStats(Handler<Void> handler) {
		return lnkStats.addClickHandler((e)->handler.handle(null));
	}
	
	public void showStats(TopVoters topVoters, UserVoteCount myStats) {
		container.clear();
		VoteStats stats = new VoteStats();
		stats.setStats(topVoters, myStats);
		container.add(stats);
	}
	
	@UiField
	protected MaterialContainer container;
	public MaterialContainer container() {
		return container;
	}
	
	private Handler<List<AudioData>> voteHandler=(d)->{};
	public HandlerRegistration votesSubmitted(Handler<List<AudioData>> handler) {
		if (handler==null) {
			voteHandler=(d)->{};
		} else {
			voteHandler=handler;
		}
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				voteHandler=(d)->{};
			}
		};
	}
	
	@Inject
	protected RunAsync async;
	
	private static class Group {
		AudioData data;
		MaterialRadioButton btnBad;
		MaterialRadioButton btnPoor;
		MaterialRadioButton btnGood;
	}
	
	private List<Group> groups = new ArrayList<>();
	public void setAudioDataList(AudioDataList list) {
		if (list.getList().isEmpty()) {
			showNoFiles();
			return;
		}
		container.clear();
		groups.clear();
		for (AudioData item: list) {
			container.add(new Hr());
			
			Group group = new Group();
			group.data=item;
			
			MaterialRow fileNameRow = new MaterialRow();
			
			MaterialColumn fileNameColumn = new MaterialColumn();
			String audioFile = item.getFile();
			int last = audioFile.lastIndexOf("/");
			if (last>-1) {
				audioFile=audioFile.substring(last+1);
			}
			MaterialLabel filenameText = new MaterialLabel(audioFile);
			filenameText.setFontWeight(FontWeight.NORMAL);
			fileNameColumn.add(filenameText);
			fileNameRow.add(fileNameColumn);
			
			container.add(fileNameRow);
			
			MaterialRow textRow = new MaterialRow();
			
			MaterialColumn c0 = new MaterialColumn();
			MaterialLabel text = new MaterialLabel(item.getTxt().trim());
			text.setFontWeight(FontWeight.BOLD);
			text.setFontSize("125pct");
			c0.add(text);
			textRow.add(c0);
			
			container.add(textRow);
			
			MaterialRow row = new MaterialRow();
			
			MaterialColumn c1 = new MaterialColumn(); 
			HTML audio = new HTML("<audio controls src='"+item.getUrl()+"' />");
			c1.add(audio);
			row.add(c1);
			
			MaterialColumn c;
			
			String radioButtonGroup = "vote-"+item.getVid();
			
			c = new MaterialColumn();
			MaterialRadioButton btnBad = new MaterialRadioButton(radioButtonGroup);
			group.btnBad=btnBad;
			btnBad.setText("Bad");
			c.add(btnBad);
			row.add(c);
			
			c = new MaterialColumn();
			MaterialRadioButton btnPoor = new MaterialRadioButton(radioButtonGroup);
			group.btnPoor=btnPoor;
			btnPoor.setText("Poor");
			c.add(btnPoor);
//			row.add(c);
			
			c = new MaterialColumn();
			MaterialRadioButton btnGood = new MaterialRadioButton(radioButtonGroup);
			group.btnGood=btnGood;
			btnGood.setText("Good");
			c.add(btnGood);
			row.add(c);
			
			c = new MaterialColumn();
			MaterialRadioButton btnNoVote = new MaterialRadioButton(radioButtonGroup);
			btnNoVote.setText("No Vote");
			btnNoVote.setValue(true);
			c.add(btnNoVote);
			row.add(c);
			
			c = new MaterialColumn();
			
			groups.add(group);
			container.add(row);
		}
		container.add(new Hr());
		
		MaterialRow row = new MaterialRow();
		MaterialColumn c;
		MaterialButton cancel = new MaterialButton("CANCEL");
		cancel.addClickHandler((e)->container.clear());
		c = new MaterialColumn();
		c.add(cancel);
		row.add(c);
		
		MaterialButton submit = new MaterialButton("SUBMIT");
		List<AudioData> dataList = new ArrayList<>();
		submit.addClickHandler((e)->{
			for (Group group: groups) {
				MaterialRadioButton btnBad = group.btnBad;
				MaterialRadioButton btnGood = group.btnGood;
				MaterialRadioButton btnPoor = group.btnPoor;
				AudioData data = group.data;
				data.setBad(btnBad.getValue()?1:0);
				data.setPoor(btnPoor.getValue()?1:0);
				data.setGood(btnGood.getValue()?1:0);
				dataList.add(data);
			}
			async.run(()->voteHandler.handle(dataList));
			container.clear();
		});
		c = new MaterialColumn();
		c.add(submit);
		row.add(c);
		container.add(row);
	}

	private void showNoFiles() {
		container.clear();
		Heading h = new Heading(HeadingSize.H5);
		h.setText("No audio tracks found that you haven't voted on. Try back later.");
		container.add(h);
	}

	public void showAbout() {
		container.clear();
		container.add(new AboutDisplay());
	}

	public Settings showSettings() {
		Settings settingsView = new Settings();
		container.clear();
		container.add(settingsView);
		return settingsView;
	}

	public RecordAudio showRecordView() {
		container.clear();
		RecordAudio recView = new RecordAudio();
		container.add(recView);
		return recView;
	}
}
