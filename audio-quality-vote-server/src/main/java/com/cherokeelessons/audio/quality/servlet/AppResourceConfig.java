package com.cherokeelessons.audio.quality.servlet;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

@ApplicationPath("api")
public class AppResourceConfig extends ResourceConfig {
	public AppResourceConfig() {
		EncodingFilter.enableFor(this, GZipEncoder.class);
		register(JacksonProvider.class);
		register(RestApiImpl.class);
	}
}
