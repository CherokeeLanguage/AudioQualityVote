package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public interface RestApi {
	@Path("user/login")
	@POST
	UserInfo login(@HeaderParam("id-token")String idToken);
	
	@Path("users/session-id")
	@GET
	Boolean isSessionId(@HeaderParam("uid")Long uid, @HeaderParam("session-id")String sessionId);
	
	@Path("user/logout")
	@GET
	void logout(@HeaderParam("uid")Long uid, @HeaderParam("session-id")String sessionId);
	
	@Produces("audio/mpeg")
	@Path("audio/file/{id}")
	@GET
	Object audioGet(@PathParam("id")String id);
	
	@Path("audio/info/{id}")
	@GET
	AudioData audioInfo(@HeaderParam("uid")Long uid, @HeaderParam("session-id")String sessionId, @PathParam("id")String id);
	
	@Path("audio/vote/{vid}/{bad}/{poor}/{good}")
	@POST
	AudioData audioVote(@HeaderParam("uid")Long uid, @HeaderParam("session-id")String sessionId, @PathParam("vid")Long vid, @PathParam("bad")Integer bad, @PathParam("poor")Integer poor, @PathParam("good")Integer good);
	
	@Path("audio/list/{qty}")
	@GET
	AudioDataList audioList(@HeaderParam("uid")Long uid, @HeaderParam("session-id")String sessionId, @PathParam("qty") Integer qty);
}
