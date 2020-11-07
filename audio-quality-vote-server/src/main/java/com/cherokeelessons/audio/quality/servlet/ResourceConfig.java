package com.cherokeelessons.audio.quality.servlet;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.filter.EncodingFilter;

@ApplicationPath("/api")
public class ResourceConfig extends org.glassfish.jersey.server.ResourceConfig {
	public ResourceConfig() {
		EncodingFilter.enableFor(this, GZipEncoder.class);
		register(RestApiImpl.class);
	}
}
