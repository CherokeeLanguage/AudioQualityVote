package com.cherokeelessons.audio.quality.shared;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

public interface RestApi {
	@Path("audio/user/list")
	@GET
	UserAudioList audioUserList(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Path("audio/user/text/get/{count}")
	@GET
	TextForRecording audioText(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("count") Integer count);

	@Path(ApiPaths.AUDIO_PUT)
	@PUT
	Object audioPut(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@QueryParam("text") String text);

	@Path("audio/user/delete/{aid}")
	@DELETE
	void audioDelete(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("aid") Long aid);

	@Path("user/delete")
	@POST
	void deleteSelf(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Path("user/login")
	@POST
	UserInfo login(@HeaderParam("id-token") String idToken);

	@Path("user/session-id")
	@GET
	Boolean isSessionId(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Path("user/logout")
	@GET
	void logout(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Produces("audio/fetch")
	@Path(ApiPaths.AUDIO_FILE)
	@GET
	Object audioGet(@PathParam("aid") String aid);

	@Path("audio/details/{aid}")
	@GET
	AudioData audioData(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("aid") Long aid);

	@Path("audio/vote/{vid}/{bad}/{poor}/{good}")
	@POST
	AudioData audioVote(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId,
			@PathParam("vid") Long vid, @PathParam("bad") Integer bad, @PathParam("poor") Integer poor,
			@PathParam("good") Integer good);

	@Path("audio/vote/my/counts")
	@GET
	UserVoteCount myVoteCounts(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	@Path("user/top-voters")
	@GET
	TopVoters topVoters(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

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
	@Path(ApiPaths.VOTES_CSV)
	@GET
	Object audioQualityVotesCsv();

	@Path("audio/list/count")
	@GET
	Total audioTrackCount(@HeaderParam("uid") Long uid, @HeaderParam("session-id") String sessionId);

	interface ApiPaths {
		String AUDIO_FILE = "audio/file/{aid}";
		String VOTES_CSV = "audio/votes/list/AudioQualityVotes.csv";
		String AUDIO_PUT = "audio/user/put";
	}
}
