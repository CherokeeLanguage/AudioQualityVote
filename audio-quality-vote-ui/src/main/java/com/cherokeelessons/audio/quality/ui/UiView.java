package com.cherokeelessons.audio.quality.ui;

import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IsWidget;

public interface UiView extends IsWidget {
	@SuppressWarnings("unchecked")
	static <T extends UiView> T uncheckedCast(UiView _this) {
		return (T) _this;
	}
 
	String getWindowTitle();
 
	default void enable(boolean enabled) {
		UiViewUtils.setEnabled(this, enabled);
	}
	
	//String getHref();
	//void setHref(String href);
}
 
class UiViewUtils {
	protected static void setEnabled(IsWidget widget, boolean enabled) {
		if (widget instanceof HasEnabled) {
			((HasEnabled) widget).setEnabled(enabled);
		}
		if (widget instanceof HasWidgets) {
			((HasWidgets) widget).forEach(w -> setEnabled(widget, enabled));
		}
	}
}