package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

import org.dominokit.domino.rest.shared.request.service.annotations.RequestFactory;

@RequestFactory
public interface RestApi {
	@Path("user/login")
	@GET
	String login(@HeaderParam("oauth")String oauth);
	
	@Path("user/logout")
	@GET
	void logout(@HeaderParam("sessionid")String token);
	
	default void x() {
		RestApiFactory.INSTANCE.login(null);
	}
}
