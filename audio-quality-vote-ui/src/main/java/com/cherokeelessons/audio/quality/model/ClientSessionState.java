package com.cherokeelessons.audio.quality.model;

import java.util.Map;

public class ClientSessionState {
	private static final String SESSION_ID = "sessionId";
	private static final String OAUTH = "oauth";
	private static final String EMAIL = "email";
	private static final String UID = "uid";
	
	public ClientSessionState(Map<String, String> map) {
		this.map=map;
	}
	private final Map<String, String> map;
	
	public Map<String, String> map() {
		return map;
	}
	
	public String sessionId() {
		return map.getOrDefault(SESSION_ID, "");
	}
	
	public void clearSessionId() {
		map.remove(SESSION_ID);
	}
	
	public void sessionId(String sessionId) {
		if (sessionId==null) {
			clearSessionId();
			return;
		}
		map.put(SESSION_ID, sessionId);
	}
	
	public Long uid() {
		try {
			return Long.valueOf(map.getOrDefault(UID, "0"));
		} catch (NumberFormatException e) {
			return 0l;
		} 
	}
	
	public void uid(Long uid) {
		if (uid==null) {
			clearUid();
			return;
		}
		map.put(UID, uid.toString());
	}
	
	private void clearUid() {
		map.remove(UID);
	}

	public String oauth() {
		return map.getOrDefault(OAUTH, "");
	}
	
	public void clearOauth() {
		map.remove(OAUTH);
	}
	
	public void oauth(String oauth) {
		if (oauth==null) {
			clearOauth();
			return;
		}
		map.put(OAUTH, oauth);
	}
 
	public void clear() {
		this.map.clear();
	}

	public String email() {
		return map.getOrDefault(EMAIL, "");
	}
	
	public void email(String email) {
		if (email==null) {
			clearEmail();
			return;
		}
		map.put(EMAIL, email);
	}

	private void clearEmail() {
		map.remove(EMAIL);
	}
}
