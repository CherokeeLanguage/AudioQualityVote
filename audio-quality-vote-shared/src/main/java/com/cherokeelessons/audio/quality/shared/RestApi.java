package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public interface RestApi {
	@Path("user/login")
	@GET
	String login(@HeaderParam("oauth")String oauth);
	
	@Path("user/logout")
	@GET
	void logout(@HeaderParam("sessionid")String token);
	
	@Path("audio/mp3/{id}")
	@GET
	@Produces("audio/mpeg")
	Object audioGet(@HeaderParam("sessionid")String token, @PathParam("id")String id);
	
	@Path("audio/info/{id}")
	@GET
	String audioInfo(@HeaderParam("sessionid")String token, @PathParam("id")String id);
	
	@Path("audio/vote/{id}/{vote}")
	String audioVote(@HeaderParam("sessionid")String token, @PathParam("id")String id, @PathParam("vote")String vote);
	
	@Path("audio/list")
	String audioList(@HeaderParam("sessionid")String token);
}
