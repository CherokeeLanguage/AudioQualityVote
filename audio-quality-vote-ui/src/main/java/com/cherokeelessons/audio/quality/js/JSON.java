package com.cherokeelessons.audio.quality.js;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative=true, namespace=JsPackage.GLOBAL)
public class JSON {
	public native static String stringify(Object obj);	
	public native static Object parse(String obj);
}