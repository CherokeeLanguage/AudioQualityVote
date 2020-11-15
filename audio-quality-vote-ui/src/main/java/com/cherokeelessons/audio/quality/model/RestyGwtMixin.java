package com.cherokeelessons.audio.quality.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.nmorel.gwtjackson.client.AbstractConfiguration;

public class RestyGwtMixin extends AbstractConfiguration {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MixInIgnoreUnknownProperties {
		//
	}

	public RestyGwtMixin() {
		super();
	}

	@Override
	protected void configure() {
		addMixInAnnotations(Object.class, MixInIgnoreUnknownProperties.class);
	}
}
