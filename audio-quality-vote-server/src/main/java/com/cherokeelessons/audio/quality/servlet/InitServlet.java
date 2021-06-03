package com.cherokeelessons.audio.quality.servlet;

import java.io.File;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.cherokeelessons.audio.quality.db.AudioQualityVoteDao;
import com.cherokeelessons.audio.quality.db.AudioQualityVoteFiles;
import com.cherokeelessons.audio.quality.shared.AudioBytesInfo;

@SuppressWarnings("serial")
@WebServlet(value = "/init-servlet", loadOnStartup = 1)
public class InitServlet extends HttpServlet {
	@Override
	public void init() throws ServletException {
		super.init();
//		new Thread(this::migrate).start();
	}

	private void migrate() {
		System.out.println("Migration start.");
		List<AudioBytesInfo> temp = dao().audioVoteEntriesForMigration();
		temp.forEach(data -> {
			File file = new File(AudioQualityVoteFiles.getFolder(), data.getFile());
			if (!file.exists()) {
				return;
			}
			data.setUid(0);
			data.setMime("audio/mpeg");
			if (dao().audioBytesInfoExistsFor(data.getUid(), data.getFile())) {
				Long aid = dao().getAidForFile(data.getFile());
				if (aid != null && aid > 0) {
					System.out.println("Migrating " + file.getName() + ".");
					dao().setAudioIdForMatchingVotes(aid, data.getFile());
				}
				return;
			}
			long aid = dao().addAudioBytesInfo(data);
			if (aid < 1) {
				return;
			}
			System.out.println("Migrating " + file.getName() + ".");
			dao().setAudioBytesData(aid, file);
			dao().setAudioIdForMatchingVotes(aid, data.getFile());
		});
		System.out.println("Migration complete.");
	}

	@Override
	public void destroy() {
		AudioQualityVoteDao.close();
		super.destroy();
	}

	private AudioQualityVoteDao dao() {
		return AudioQualityVoteDao.onDemand();
	}
}
