package com.cherokeelessons.audio.quality.servlet;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.ws.rs.Path;

import com.cherokeelessons.audio.quality.shared.AudioInfo;
import com.cherokeelessons.audio.quality.shared.AudioInfoList;
import com.cherokeelessons.audio.quality.shared.Consts;
import com.cherokeelessons.audio.quality.shared.RestApi;
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Path("/")
public class RestApiImpl implements RestApi {

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
		UserInfo info = new UserInfo();
		info.setEmail(token.getPayload().getEmail());
		info.setId(token.getPayload().getSubject());
		return info;
	}

	@Override
	public void logout(String sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object audioGet(String sessionId, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioInfo audioInfo(String sessionId, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioInfo audioVote(String sessionId, String id, Integer vote) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AudioInfoList audioList(String sessionId, Integer qty) {
		// TODO Auto-generated method stub
		return null;
	}

}
