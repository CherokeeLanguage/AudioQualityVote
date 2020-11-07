package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

public interface RestApi {
	@Path("user/login")
	@GET
	String login(@HeaderParam("oauth")String oauth);
	
	@Path("user/logout")
	@GET
	void logout(@HeaderParam("sessionid")String token);
}
