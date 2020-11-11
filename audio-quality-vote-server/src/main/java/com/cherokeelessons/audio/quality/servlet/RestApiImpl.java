package com.cherokeelessons.audio.quality.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Path("/")
public class RestApiImpl implements RestApi {
	
	@Context
	protected HttpSession session;

	@Context
	protected HttpServletRequest request;
	
	protected HttpServletResponse response;
	
	@Context
	protected void setResponse(HttpServletResponse response) {
		this.response=response;
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
		GoogleIdTokenVerifier verifier =
			    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
	            .setAudience(Arrays.asList(Consts.CLIENT_ID))
	            .setIssuer("accounts.google.com")
	            .build();
		GoogleIdToken token;
		try {
			token = verifier.verify(idToken);
		} catch (GeneralSecurityException | IOException e) {
			return null;
		}
		if (token==null) {
			return null;
		}
		String oauthId = token.getPayload().getSubject();
		String email = token.getPayload().getEmail();
		dao().addUser("Google", oauthId, email);
		dao().updateEmail("Google", oauthId, email);
		long uid = dao().uid(email);
		UserInfo info = new UserInfo();
		info.setUid(uid);
		info.setEmail(email);
		info.setSessionId(dao().newSessionId(uid));
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
		if (data==null) {
			System.err.println("No entry in db for vid "+vid);
			return Response.status(Status.NOT_FOUND).build();
		}
		File file = new File(AudioQualityVoteFiles.getFolder(), data.getAudioFile());
		if (!file.exists()) {
			System.err.println("No file found for vid "+vid);
			System.out.println(file.getAbsolutePath());
			return Response.status(Status.NOT_FOUND).build();
		}
		try {
			response.setHeader("Cache-Control", "public,max-age="+(60*60*24));
			return Response.ok(new FileInputStream(file)).build();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public AudioData audioInfo(Long uid, String sessionId, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioDataList audioList(Long uid, String sessionId, Integer qty) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		if (qty==null || qty<1) {
			qty=16;
		}
		List<Integer> pending = dao().pending(uid);
		Collections.shuffle(pending);
		if (pending.size()>qty) {
			pending = pending.subList(0, qty);
		}
		AudioDataList list = new AudioDataList();
		if (pending.isEmpty()) {
			return list;
		}
		for (Integer vid: pending) {
			list.getList().add(dao().audioData(vid));
		}
		return list;
	}
	
	@Override
	public Boolean isSessionId(Long uid, String sessionId) {
		if (uid==null || sessionId==null) {
			return false;
		}
		return dao().isSessionId(uid, sessionId);
	}

	@Override
	public AudioData audioVote(Long uid, String sessionId, Long vid, Integer bad, Integer poor, Integer good) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		dao().setVote(uid, vid, bad, poor, good);
		return dao().audioData(vid);
	}
}
