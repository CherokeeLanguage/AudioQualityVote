package com.cherokeelessons.audio.quality.ui;

import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLMetaElement;

/**
 * The deployed artifact's stamped build metadata — {@code aqv-version} and
 * {@code aqv-build-date} on the host page (AudioQualityVote#15). The host page is
 * stamped at build time, so these values describe the server that serves this page,
 * not the repo HEAD a merge has advanced past.
 */
public class BuildInfo {

	public static String version() {
		return meta("aqv-version");
	}

	public static String buildDate() {
		return meta("aqv-build-date");
	}

	/** The render string both views display: "Version <v> — built <d>", empty when unstamped. */
	public static String displayText() {
		String version = version();
		String date = buildDate();
		return version.isEmpty() || date.isEmpty() ? "" : "Version " + version + " — built " + date;
	}

	private static String meta(String name) {
		Element element = DomGlobal.document.querySelector("meta[name='" + name + "']");
		if (!(element instanceof HTMLMetaElement)) {
			return "";
		}
		String content = ((HTMLMetaElement) element).content;
		return content == null ? "" : content;
	}
}