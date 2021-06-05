package com.cherokeelessons.audio.quality.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.audio.quality.db.AppPathConfig;
import com.cherokeelessons.audio.quality.db.AudioQualityVoteDao;
import com.cherokeelessons.audio.quality.db.AudioQualityVoteFiles;
import com.cherokeelessons.audio.quality.shared.AudioBytesInfo;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.AudioDataList;
import com.cherokeelessons.audio.quality.shared.Consts;
import com.cherokeelessons.audio.quality.shared.RestApi;
import com.cherokeelessons.audio.quality.shared.TextForRecording;
import com.cherokeelessons.audio.quality.shared.TopVoters;
import com.cherokeelessons.audio.quality.shared.Total;
import com.cherokeelessons.audio.quality.shared.UserAudioList;
import com.cherokeelessons.audio.quality.shared.UserInfo;
import com.cherokeelessons.audio.quality.shared.UserVoteCount;
import com.cherokeelessons.audio.quality.shared.VoteResult;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
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
		return AudioQualityVoteDao.onDemand(AppPathConfig.TABLE_PREFIX);
	}

	private static final ReentrantLock LOAD_CHECK = new ReentrantLock();

	public RestApiImpl() {
		Thread thread = new Thread(this::audioLoadCheck);
		thread.setDaemon(true);
		thread.start();
	}

	private void audioLoadCheck() {
		try {
			if (!LOAD_CHECK.tryLock()) {
				return;
			}
			System.out.println("Audio Load Check - START");
			final String audioDataPath = AudioQualityVoteFiles.getFolder().getAbsolutePath();
			List<AudioData> audioData = AudioQualityVoteFiles.getAudioData();
			System.out.printf(" - Found %,d audio files with matching text.\n", audioData.size());
			for (AudioData d : audioData) {
				String file = d.getFile();
				File dataFile = new File(file);
				file = StringUtils.substringAfter(file, audioDataPath);
				d.setFile(file);
				if (dao().getAidForFile(AppPathConfig.TABLE_PREFIX, file) != null) {
					continue;
				}
				AudioBytesInfo info = new AudioBytesInfo();
				info.setFile(file);
				info.setMime("audio/mpeg");
				info.setTxt(Normalizer.normalize(d.getTxt().trim(), Form.NFC));
				info.setUid(0);
				long aid = dao().insertAudioBytesInfo(AppPathConfig.TABLE_PREFIX, info);
				if (aid < 1) {
					continue;
				}
				dao().setAudioBytesData(AppPathConfig.TABLE_PREFIX, aid, dataFile);
			}
			System.out.println("Audio Load Check - DONE");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (LOAD_CHECK.isHeldByCurrentThread()) {
				LOAD_CHECK.unlock();
			}
		}
	}

	@Override
	public UserInfo login(String idToken) {
		NetHttpTransport transport = new NetHttpTransport();
		JsonFactory factory = new GsonFactory();
		List<String> audience = Arrays.asList(Consts.CLIENT_ID);
		String issuer = "accounts.google.com";
		GoogleIdTokenVerifier verifier = //
				new GoogleIdTokenVerifier.Builder(transport, factory) //
						.setAudience(audience).setIssuer(issuer).build();
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
		dao().addUser(AppPathConfig.TABLE_PREFIX, "Google", oauthId, email);
		dao().updateEmail(AppPathConfig.TABLE_PREFIX, "Google", oauthId, email);
		long uid = dao().uidByEmail(AppPathConfig.TABLE_PREFIX, email);
		dao().updateLastLogin(AppPathConfig.TABLE_PREFIX, uid);
		dao().scanForNewEntries(AppPathConfig.TABLE_PREFIX, uid);
		UserInfo info = new UserInfo();
		info.setUid(uid);
		info.setEmail(email);
		info.setSessionId(dao().newSessionId(AppPathConfig.TABLE_PREFIX, uid));
		int sessionCount = dao().sessionCount(AppPathConfig.TABLE_PREFIX, uid);
		if (sessionCount > 5) {
			dao().deleteOldestSessions(AppPathConfig.TABLE_PREFIX, uid, sessionCount - 5);
		}
		return info;
	}

	@Override
	public void logout(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return;
		}
		dao().deleteSessionId(AppPathConfig.TABLE_PREFIX, uid, sessionId);
	}

	@Override
	public Response audioGet(String id) {
		Long aid;
		try {
			aid = Long.parseLong(id);
		} catch (NumberFormatException e) {
			return Response.status(Status.NOT_FOUND).build();
		}
		AudioBytesInfo data = dao().audioBytesInfo(AppPathConfig.TABLE_PREFIX, aid);
		if (data == null) {
			System.err.println("No entry in db for aid " + aid);
			return Response.status(Status.NOT_FOUND).build();
		}
		if (!dao().audioBytesInfoHasData(AppPathConfig.TABLE_PREFIX, aid)) {
			System.err.println("Entry in db for aid " + aid + " does not have audio data attached.");
			return Response.status(Status.NOT_FOUND).build();
		}

		try {
			String audioBytesMime = StringUtils.defaultString(dao().audioBytesMime(AppPathConfig.TABLE_PREFIX, aid), "audio/mpeg");
			response.setHeader("Cache-Control", "public,max-age=" + (60 * 60 * 24));
			response.setContentType(audioBytesMime);
			dao().audioBytesStream(AppPathConfig.TABLE_PREFIX, aid, response.getOutputStream());
			return Response.ok().type(audioBytesMime).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	@Override
	public AudioData audioData(Long uid, String sessionId, Long aid) {
		if (!isSessionId(uid, sessionId) || aid == null) {
			return null;
		}
		return dao().audioDataInfoByAid(AppPathConfig.TABLE_PREFIX, aid);
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
			List<Long> pending = dao().pendingVids(AppPathConfig.TABLE_PREFIX, uid);
			if (pending.isEmpty()) {
				return list;
			}
			Collections.shuffle(pending);
			if (pending.size() > qty) {
				pending = pending.subList(0, qty);
			}
			for (Long vid : pending) {
				list.getList().add(dao().audioDataInfoByVid(AppPathConfig.TABLE_PREFIX, vid));
			}
		} while (list.getList().isEmpty());
		return list;
	}

	@Override
	public Boolean isSessionId(Long uid, String sessionId) {
		if (uid == null || sessionId == null) {
			return false;
		}
		if (Boolean.TRUE == dao().isSessionId(AppPathConfig.TABLE_PREFIX, uid, sessionId)) {
			dao().updateLastSeen(AppPathConfig.TABLE_PREFIX, uid, sessionId);
			dao().deleteOldSessions(AppPathConfig.TABLE_PREFIX);
			return true;
		}
		return false;
	}

	@Override
	public AudioData audioVote(Long uid, String sessionId, Long vid, Integer bad, Integer poor, Integer good) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		dao().setVote(AppPathConfig.TABLE_PREFIX, uid, vid, bad, poor, good);
		return dao().audioDataInfoByVid(AppPathConfig.TABLE_PREFIX, vid);
	}

	@Override
	public AudioDataList audioListBrowse(Long uid, String sessionId, Integer size, Integer page) {
		if (!isSessionId(uid, sessionId) || page == null || size == null) {
			return null;
		}
		List<Long> vids = dao().audioDataVidsFor(AppPathConfig.TABLE_PREFIX, uid);
		AudioDataList list = new AudioDataList();
		for (Long vid : vids) {
			list.getList().add(dao().audioDataInfoByVid(AppPathConfig.TABLE_PREFIX, vid));
		}
		list.getList().forEach(item -> {
			item.setTxt(Normalizer.normalize(item.getTxt(), Form.NFC));
		});
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
		List<VoteResult> voteResults = dao().audioVoteResults(AppPathConfig.TABLE_PREFIX);
		String[] header = { "Id", "Bad", "Poor", "Good", "Ranking", "Votes", "Text", "File" };
		try (StringWriter writer = new StringWriter(); CSVWriter csv = new CSVWriter(writer)) {
			csv.writeNext(header);
			for (VoteResult result : voteResults) {
				AudioBytesInfo info = dao().audioBytesInfo(AppPathConfig.TABLE_PREFIX, result.getAid());
				info.setTxt(Normalizer.normalize(info.getTxt(), Form.NFC));
				String[] row = { //
						result.getAid() + "", //
						result.getBad() + "", //
						result.getPoor() + "", //
						result.getGood() + "", //
						result.getRanking() + "", //
						result.getVotes() + "", //
						info.getTxt(), //
						info.getFile() //
				};
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
		counts.setPending(dao().userPendingVoteCount(AppPathConfig.TABLE_PREFIX, uid));
		counts.setVoted(dao().userCompletedVoteCount(AppPathConfig.TABLE_PREFIX, uid));
		counts.setUid(uid);
		return counts;
	}

	@Override
	public TopVoters topVoters(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		TopVoters topVoters = new TopVoters();
		List<Long> uids = dao().topUsersByVoteCounts(AppPathConfig.TABLE_PREFIX, 3);
		for (Long topUid : uids) {
			UserVoteCount counts = new UserVoteCount();
			counts.setPending(dao().userPendingVoteCount(AppPathConfig.TABLE_PREFIX, topUid));
			counts.setVoted(dao().userCompletedVoteCount(AppPathConfig.TABLE_PREFIX, topUid));
			counts.setUid(topUid);
			topVoters.getTopVoters().add(counts);
		}
		Collections.sort(topVoters.getTopVoters(), (a, b) -> Integer.compare(b.getVoted(), a.getVoted()));
		return topVoters;
	}

	@Override
	public void deleteSelf(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return;
		}
		dao().deleteUserById(AppPathConfig.TABLE_PREFIX, uid);
	}

	@Override
	public UserAudioList audioUserList(Long uid, String sessionId) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		UserAudioList list = new UserAudioList();
		list.setList(dao().audioBytesInfoFor(AppPathConfig.TABLE_PREFIX, uid));
		return list;
	}

	@Override
	public TextForRecording audioText(Long uid, String sessionId, Integer count) {
		if (!isSessionId(uid, sessionId)) {
			return null;
		}
		TextForRecording tfr = new TextForRecording();
		List<String> texts = dao().availableTexts(AppPathConfig.TABLE_PREFIX);
		texts.removeAll(dao().userTexts(AppPathConfig.TABLE_PREFIX, uid));
		if (texts.size() > count) {
			texts = texts.subList(0, count);
		}
		tfr.setList(texts);
		return tfr;
	}

	void sendError(Status status) {
		sendError(status, null);
	}

	void sendError(Status status, String msg) {
		try {
			if (msg == null) {
				response.sendError(status.getStatusCode());
			} else {
				response.sendError(status.getStatusCode(), msg);
			}
		} catch (IOException e) {
		}
	}

	@Override
	public AudioBytesInfo audioPut(Long uid, String sessionId, String text) {
		if (!isSessionId(uid, sessionId)) {
			sendError(Status.UNAUTHORIZED);
			return null;
		}
		if (!dao().isValidText(AppPathConfig.TABLE_PREFIX, text)) {
			sendError(Status.BAD_REQUEST, "Unknown text: " + text);
			return null;
		}
		InputStream is;
		try {
			is = request.getInputStream();
		} catch (IOException e) {
			sendError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		}
		long aid = -1;
		String file = dao().fileForText(AppPathConfig.TABLE_PREFIX, text);
		AudioBytesInfo info = new AudioBytesInfo();
		info.setFile(file);
		info.setMime(request.getContentType());
		info.setTxt(Normalizer.normalize(text, Form.NFC));
		info.setUid(uid);
		info.setAid(dao().addAudioBytesInfo(AppPathConfig.TABLE_PREFIX, info));
		aid = info.getAid();
		System.out.println("New audio id: " + info.getAid());
		dao().setAudioBytesData(AppPathConfig.TABLE_PREFIX, aid, is);
		return info;
	}

	@Override
	public void audioDelete(Long uid, String sessionId, Long aid) {
		// TODO Auto-generated method stub

	}
}
