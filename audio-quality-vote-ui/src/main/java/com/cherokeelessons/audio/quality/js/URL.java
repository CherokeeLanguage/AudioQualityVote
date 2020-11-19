package com.cherokeelessons.audio.quality.js;

import elemental2.dom.Blob;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "URL", namespace = JsPackage.GLOBAL)
public class URL {

	/**
	 * Releases an existing object URL which was previously created by calling
	 * {@link #createObjectURL(org.vectomatic.file.Blob)} . Call this method when
	 * you've finished using a object URL, in order to let the browser know it
	 * doesn't need to keep the reference to the file any longer. <a href=
	 * 'https://github.com/laaglu/lib-gwt-file/blob/master/src/main/java/org/vectomatic/file/FileUtils.java'>https://github.com/laaglu/lib-gwt-file/blob/master/src/main/java/org/vectomatic/file/FileUtils.java</a>
	 * 
	 * @param url
	 *            a string representing the object URL that was created by calling
	 *            {@link #createObjectURL(org.vectomatic.file.Blob)}
	 */
	public static native void revokeObjectURL(String objectUrl);

	/**
	 * Creates a new object URL, whose lifetime is tied to the document in the
	 * window on which it was created. The new object URL represents the specified
	 * Blob object. <a href=
	 * 'https://github.com/laaglu/lib-gwt-file/blob/master/src/main/java/org/vectomatic/file/FileUtils.java'>https://github.com/laaglu/lib-gwt-file/blob/master/src/main/java/org/vectomatic/file/FileUtils.java</a>
	 * 
	 * @param blob
	 *            the blob to represent
	 * @return a new object URL representing the blob.
	 */
	public static native String createObjectURL(Blob objectUrl);
}