package com.cherokeelessons.audio.quality.model;

import java.util.Map;

public class ClientSessionState {
	private static final String SESSIONID_ID = "sessionId";
	private static final String OAUTH = "oauth";
	
	public ClientSessionState(Map<String, String> map) {
		this.map=map;
	}
	private final Map<String, String> map;
	
	public Map<String, String> map() {
		return map;
	}
	
	public String sessionId() {
		return map.get(SESSIONID_ID);
	}
	
	public void clearSessionId() {
		map.remove(SESSIONID_ID);
	}
	
	public void sessionId(String sessionId) {
		if (sessionId==null) {
			clearSessionId();
			return;
		}
		map.put(SESSIONID_ID, sessionId);
	}
	
	public String oauth() {
		return map.get(OAUTH);
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
}
