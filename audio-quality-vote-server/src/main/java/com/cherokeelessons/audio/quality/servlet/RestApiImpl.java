package com.cherokeelessons.audio.quality.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.cherokeelessons.audio.quality.db.AudioQualityVoteDao;
import com.cherokeelessons.audio.quality.db.AudioQualityVoteFiles;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
import com.cherokeelessons.audio.quality.shared.Consts;
import com.cherokeelessons.audio.quality.shared.RestApi;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.Total;
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.cherokeelessons.audio.quality.shared.VoteResult;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.opencsv.CSVWriter;

@Path("/")
public class RestApiImpl implements RestApi {
	
	@Context
	protected HttpSession session;

	@Context
	protected HttpServletRequest request;

	protected HttpServletResponse response;

	@Context
	protected void setResponse(HttpServletResponse response) {
		this.response = response;
		response.addHeader("Cache-Control", "no-store");
	}

	@Context
	protected HttpHeaders headers;

	private AudioQualityVoteDao dao() {
		return AudioQualityVoteDao.onDemand();
	}

	public RestApiImpl() {
	}

	@Override
	public UserInfo login(String idToken) {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
				.setAudience(Arrays.asList(Consts.CLIENT_ID)).setIssuer("accounts.google.com").build();
		GoogleIdToken token;
		try {
			token = verifier.verify(idToken);
		} catch (GeneralSecurityException | IOException e) {
			return null;
		}
		if (token == null) {
			return null;
		}
		String oauthId = token.getPayload().getSubject();
		String email = token.getPayload().getEmail();
		dao().addUser("Google", oauthId, email);
		dao().updateEmail("Google", oauthId, email);
		long uid = dao().uid(email);
		dao().updateLastLogin(uid);
		dao().scanForNewFiles(uid);
		UserInfo info = new UserInfo();
		info.setUid(uid);
		info.setEmail(email);
		info.setSessionId(dao().newSessionId(uid));
		int sessionCount = dao().sessionCount(uid);
		if (sessionCount > 5) {
			dao().deleteOldestSessions(uid, sessionCount - 5);
		}
		return info;
	}

	@Override
	public void logout(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return;
		}
		dao().deleteSessionId(uid, sessionId);
	}

	@Override
	public Response audioGet(String id) {
		int vid;
		try {
			vid = Integer.parseInt(id);
		} catch (NumberFormatException e) {
			return Response.status(Status.NOT_FOUND).build();
		}
		AudioData data = dao().audioData(vid);
		if (data == null) {
			System.err.println("No entry in db for vid " + vid);
			return Response.status(Status.NOT_FOUND).build();
		}
		File file = new File(AudioQualityVoteFiles.getFolder(), data.getAudioFile());
		if (!file.exists()) {
			System.err.println("No file found for vid " + vid);
			System.out.println(file.getAbsolutePath());
			return Response.status(Status.NOT_FOUND).build();
		}
		try {
			response.setHeader("Cache-Control", "public,max-age=" + (60 * 60 * 24));
			return Response.ok(new FileInputStream(file)).build();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public AudioData audioData(Long uid, String sessionId, Long vid) {
		if (!isSessionId(uid, sessionId) || vid == null) {
			return null;
		}
		return dao().audioData(vid);
	}

	@Override
	public AudioDataList audioListUndecided(Long uid, String sessionId, Integer qty) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		if (qty == null || qty < 1) {
			qty = 16;
		}
		AudioDataList list = new AudioDataList();
		do {
			list.getList().clear();
			List<Integer> pending = dao().pendingIds(uid);
			if (pending.isEmpty()) {
				return list;
			}
			Collections.shuffle(pending);
			if (pending.size() > qty) {
				pending = pending.subList(0, qty);
			}
			for (Integer vid : pending) {
				list.getList().add(dao().audioData(vid));
			}
		} while (list.getList().isEmpty());
		return list;
	}

	@Override
	public Boolean isSessionId(Long uid, String sessionId) {
		if (uid == null || sessionId == null) {
			return false;
		}
		if( Boolean.TRUE == dao().isSessionId(uid, sessionId)) {
			dao().updateLastSeen(uid, sessionId);
			return true;
		}
		return false;
	}

	@Override
	public AudioData audioVote(Long uid, String sessionId, Long vid, Integer bad, Integer poor, Integer good) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		dao().setVote(uid, vid, bad, poor, good);
		return dao().audioData(vid);
	}

	@Override
	public AudioDataList audioListBrowse(Long uid, String sessionId, Integer size, Integer page) {
		if (!isSessionId(uid, sessionId) || page == null || size == null) {
			return null;
		}
		List<Integer> vids = dao().audioDataIdsFor(uid);
		AudioDataList list = new AudioDataList();
		for (Integer vid : vids) {
			list.getList().add(dao().audioData(vid));
		}
		return list;
	}

	@Override
	public AudioDataList audioListVotes(Long uid, String sessionId, Integer size, Integer page) {
		if (!isSessionId(uid, sessionId) || page == null || size == null) {
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Total audioTrackCount(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		Total total = new Total(dao().audioTrackCount());
		return total;
	}

	@Override
	public Response audioQualityVotesCsv() {
		List<VoteResult> voteResults = dao().audioVoteResults();
		String[] header = { "File", "Bad", "Poor", "Good", "Ranking", "Votes" };
		try (StringWriter writer = new StringWriter(); CSVWriter csv = new CSVWriter(writer)) {
			csv.writeNext(header);
			for (VoteResult result : voteResults) {
				String[] row = { result.getFile(), //
						result.getBad() + "", result.getPoor() + "", result.getGood() + "", //
						result.getRanking() + "", result.getVotes() + "" };
				csv.writeNext(row);
			}
			csv.flush();
			return Response.ok(writer.toString(), "text/csv").build();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public UserVoteCount myVoteCounts(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		UserVoteCount counts = new UserVoteCount();
		counts.setPending(dao().userPendingVoteCount(uid));
		counts.setVoted(dao().userCompletedVoteCount(uid));
		counts.setUid(uid);
		return counts;
	}

	@Override
	public TopVoters topVoters(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		TopVoters topVoters = new TopVoters();
		List<Long> uids = dao().topUsersByVoteCounts(3);
		for (Long topUid: uids) {
			UserVoteCount counts = new UserVoteCount();
			counts.setPending(dao().userPendingVoteCount(topUid));
			counts.setVoted(dao().userCompletedVoteCount(topUid));
			counts.setUid(topUid);			
			topVoters.getTopVoters().add(counts);
		}
		Collections.sort(topVoters.getTopVoters(), 
			(a,b)->Integer.compare(b.getVoted(), a.getVoted())
		);
		return topVoters;
	}

	@Override
	public void deleteSelf(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return;
		}
		dao().deleteUserById(uid);
	}
}
