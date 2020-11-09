package com.cherokeelessons.audio.quality.model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.cherokeelessons.audio.quality.ui.UiView;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import elemental2.dom.DomGlobal;
import gwt.material.design.client.ui.MaterialDialog;
import gwt.material.design.jquery.client.api.JQuery;

public class Display {
	
	@Inject
	public Display() {
		viewStack = new ArrayList<>();
	}
	
	private void setView(UiView uiView) {
		DomGlobal.document.title=uiView.getWindowTitle()+" — Audio Quality Vote";
		addWidget(uiView.asWidget());
	}
	
	public void clear() {
		final RootPanel rootPanel = RootPanel.get();
		for (Widget widget: rootPanel) {
			if (widget instanceof MaterialDialog) {
				((MaterialDialog)widget).close();
			}
			widget.removeFromParent();
		}
		JQuery.$(rootPanel.getElement()).children("#sidenav-overlay").remove();
	}
 
	public void replace(UiView view) {
		if (!viewStack.isEmpty()) {
			final UiView remove = viewStack.remove(viewStack.size()-1);
			remove.asWidget().removeFromParent();
		}
		add(view);
	}
 
	private final List<UiView> viewStack;
	
	public void add(UiView view) {
		if (view==null) {
			return;
		}
		if (!viewStack.isEmpty()) {
			final UiView remove = activeView();
			remove.asWidget().removeFromParent();
		}
		viewStack.add(view);
		setView(view);
	}
 
	public void pop() {
		if (viewStack.size()<2) {
			//don't pop our view if it is the only one
			return;
		}
		final UiView remove = viewStack.remove(viewStack.size()-1);
		remove.asWidget().removeFromParent();
		clear();
		if (viewStack.isEmpty()) {
			return;
		}
		final UiView uiView = viewStack.get(viewStack.size()-1);
		setView(uiView);
	}
 
	public void addWidget(IsWidget widget) {
		if (widget==null) {
			return;
		}
		RootPanel.get().add(widget);
	}
 
	public UiView activeView() {
		if (viewStack.isEmpty()) {
			return null;
		}
		return viewStack.get(viewStack.size()-1);
	}
}
