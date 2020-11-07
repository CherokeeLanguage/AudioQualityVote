package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public interface RestApi {
	@Path("user/login")
	@GET
	boolean login(@HeaderParam("oauth")String oauth);
	
	@Path("user/logout")
	@GET
	void logout(@HeaderParam("sessionId")String sessionId);
	
	@Produces("audio/mpeg")
	@Path("audio/file/{id}")
	@GET
	Object audioGet(@HeaderParam("sessionId")String sessionId, @PathParam("id")String id);
	
	@Path("audio/info/{id}")
	@GET
	AudioInfo audioInfo(@HeaderParam("sessionId")String sessionId, @PathParam("id")String id);
	
	@Path("audio/vote/{id}/{vote}")
	String audioVote(@HeaderParam("sessionId")String sessionId, @PathParam("id")String id, @PathParam("vote")String vote);
	
	@Path("audio/list/{qty}")
	AudioInfoList audioList(@HeaderParam("sessionId")String sessionId, int qty);
}
