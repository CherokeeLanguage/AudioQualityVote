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
	UserInfo login(@HeaderParam("id-token") String idToken);

	@Path("users/session-id")
	@GET
	Boolean isSessionId(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Path("user/logout")
	@GET
	void logout(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Produces("audio/mpeg")
	@Path(ApiPaths.audioFile)
	@GET
	Object audioGet(@PathParam("vid") String id);

	@Path("audio/details/{vid}")
	@GET
	AudioData audioData(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("vid") Long vid);

	@Path("audio/vote/{vid}/{bad}/{poor}/{good}")
	@POST
	AudioData audioVote(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("vid") Long vid, @PathParam("bad") Integer bad, @PathParam("poor") Integer poor,
			@PathParam("good") Integer good);

	@Path("audio/list/undecided/{qty}")
	@GET
	AudioDataList audioListUndecided(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("qty") Integer qty);

	@Path("audio/list/browse/{size}/{page}")
	@GET
	AudioDataList audioListBrowse(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("size") Integer size, @PathParam("page") Integer page);

	@Path("audio/votes/list/{size}/{page}")
	@GET
	AudioDataList audioListVotes(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("size") Integer size, @PathParam("page") Integer page);
	
	@Produces("text/csv")
	@Path(ApiPaths.audioQualityVotesCsv)
	@GET
	Object audioQualityVotesCsv();

	@Path("audio/list/count")
	@GET
	AudioDataList audioListCount(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);
	
	interface ApiPaths {
		String audioFile = "audio/file/{vid}";
		String audioQualityVotesCsv="audio/votes/list/AudioQualityVotes.csv";	
	}
}
