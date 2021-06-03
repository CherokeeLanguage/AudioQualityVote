package com.cherokeelessons.audio.quality.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import com.cherokeelessons.audio.quality.shared.AudioBytesInfo;
import com.cherokeelessons.audio.quality.shared.AudioData;
import com.cherokeelessons.audio.quality.shared.VoteResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public interface AudioQualityVoteDao extends SqlObject {

	public static final int NEW_ENTRIES_COUNT = 10;
	int MIN_VOTES_FILTER = 3;

	static AudioQualityVoteDao onDemand() {
		if (State.dao != null) {
			return State.dao;
		}
		State.loadPropertiesFile();

		final HikariConfig config = new HikariConfig();
		config.setIdleTimeout(5 * 60 * 1000l);
		config.setMaximumPoolSize(Math.max(4, Math.max(Runtime.getRuntime().availableProcessors() - 1, 1)));
		config.setMaxLifetime(15 * 60 * 1000l); // min allowed
		config.setMinimumIdle(0);
		config.setInitializationFailTimeout(0);
		config.setConnectionTimeout(0);
		config.setDriverClassName("org.mariadb.jdbc.Driver");
		config.setJdbcUrl(State.jdbcUrl);
		config.setUsername(State.user);
		config.setPassword(State.password);

		State.ds = new HikariDataSource(config);

		Jdbi jdbi = Jdbi.create(State.ds);
		final SerializableTransactionRunner transactionHandler = new SerializableTransactionRunner();
		jdbi.setTransactionHandler(transactionHandler); // auto retry transactions that deadlock
		jdbi.installPlugin(new SqlObjectPlugin());
		AudioQualityVoteDao onDemand = jdbi.onDemand(AudioQualityVoteDao.class);
		onDemand.init();
		State.dao = onDemand;
		return onDemand;
	}

	static void close() {
		if (State.ds != null) {
			State.ds.close();
		}
	}

	@SqlScript("alter database character set = 'utf8mb4'")
	@SqlScript("create table if not exists aqv_users" //
			+ " (uid serial, oauth_provider varchar(254)," //
			+ " oauth_id varchar(254), email varchar(254)," //
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW()" //
			+ ")")
	@SqlScript("create index if not exists oauth_provider on aqv_users(oauth_provider(4))")
	@SqlScript("create index if not exists oauth_id on aqv_users(oauth_id(4))")
	@SqlScript("create index if not exists email on aqv_users(email(4))")
	@SqlScript("alter table aqv_users add column if not exists last_login datetime")

	@SqlScript("create table if not exists aqv_sessions" //
			+ " (sid serial, uid bigint unsigned, session varchar(254)," //
			+ " last_seen datetime default NOW() on update NOW()," //
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW()" //
			+ ")")
	@SqlScript("create index if not exists uid on aqv_sessions(uid)")
	@SqlScript("create index if not exists session on aqv_sessions(session(16))")
	@SqlScript("create index if not exists last_seen on aqv_sessions(last_seen)")

	//
	@SqlScript("create table if not exists aqv_votes" //
			+ " (vid serial," //
			+ " uid bigint unsigned," //
			+ " file varchar(254)," //
			+ " txt text," + " good int default 0," //
			+ " poor int default 0," //
			+ " bad int default 0," + " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists uid on aqv_votes(uid)")
	@SqlScript("create index if not exists file on aqv_votes(file(32))")
	@SqlScript("create index if not exists txt on aqv_votes(txt(8))")
	@SqlScript("create index if not exists modified on aqv_votes(modified)")
	@SqlScript("create index if not exists created on aqv_votes(created)")

	@SqlScript("alter table aqv_votes add column if not exists aid bigint unsigned")
	@SqlScript("create index if not exists aid on aqv_votes(aid)")

	@SqlScript("create table if not exists aqv_audio" //
			+ " (aid serial," //
			+ " uid bigint unsigned," //
			+ " file text," //
			+ " txt text," + " mime text," + " data longblob," + " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists uid on aqv_audio(uid)")
	@SqlScript("create index if not exists file on aqv_audio(file(32))")
	@SqlScript("create index if not exists txt on aqv_audio(txt(8))")
	@SqlScript("create index if not exists mime on aqv_audio(mime(16))")
	@SqlScript("create index if not exists modified on aqv_audio(modified)")
	@SqlScript("create index if not exists created on aqv_audio(created)")
	void init();

	default void audioBytesStream(long aid, OutputStream os) throws IOException {
		AudioBytesObject data = audioBytesObject(aid);
		if (data == null || data.getData() == null) {
			data = new AudioBytesObject();
			data.setData(new byte[0]);
		}
		try (InputStream bs = new ByteArrayInputStream(data.getData())) {
			IOUtils.copy(bs, os);
		}
	}

	@SqlQuery("select data from aqv_audio where aid=:aid")
	byte[] audioBytes(@Bind("aid") long aid);

	class AudioBytesObject { // needed for hacky work around to get binary data out of DB
		private byte[] data;

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}
	}

	@RegisterBeanMapper(AudioBytesObject.class)
	@SqlQuery("select data from aqv_audio where aid=:aid")
	AudioBytesObject audioBytesObject(@Bind("aid") long aid);

	@SqlUpdate("delete from aqv_audio where aid=:aid")
	void deleteAudioBytes(@Bind("aid") long aid);

	@SqlUpdate("delete from aqv_audio where uid=:uid")
	void deleteAudioBytesByUid(@Bind("uid") long uid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from aqv_audio where aid=:aid")
	@RegisterBeanMapper(AudioBytesInfo.class)
	AudioBytesInfo audioBytesInfo(@Bind("aid") long aid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from aqv_audio where uid=:uid")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioBytesInfoFor(@Bind("uid") long uid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from aqv_audio order by aid")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioBytesInfo();

	@Transaction
	default long addAudioBytesInfo(AudioBytesInfo info) {
		deleteAudioBytesInfo(info);
		return insertAudioBytesInfo(info);
	}

	@SqlUpdate("insert into aqv_audio (uid, file, txt, mime)" //
			+ " select :uid, :file, :txt, :mime from (select 1) a" //
			+ " where not exists" //
			+ " (select 1 from aqv_audio where file=:file)")
	@GetGeneratedKeys
	long insertAudioBytesInfo(@BindBean AudioBytesInfo info);

	@SqlUpdate("delete from aqv_audio where uid=:uid and file=:file")
	void deleteAudioBytesInfo(@BindBean AudioBytesInfo info);

	@SqlUpdate("update aqv_audio set data=:data where aid=:aid")
	void setAudioBytesData(@Bind("aid") long aid, @Bind("data") byte[] data);

	default void setAudioBytesData(long aid, InputStream data) {
		useHandle(h -> {
			h.createUpdate("update aqv_audio set data=:data where aid=:aid") //
					.bind("data", new ServletInputStreamArgument(data)) //
					.bind("aid", aid) //
					.execute();
		});
	}

	default void setAudioBytesData(long aid, File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			setAudioBytesData(aid, fis);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SqlQuery("select *" //
			+ " from aqv_audio where" //
			+ " aid=:aid")
	@RegisterBeanMapper(AudioData.class)
	AudioData audioDataInfoByAid(@Bind("aid") long aid);

	@SqlQuery("select *" //
			+ " from aqv_votes where" //
			+ " vid=:vid")
	@RegisterBeanMapper(AudioData.class)
	AudioData audioDataInfoByVid(@Bind("vid") long vid);

	@SqlQuery("select vid from aqv_votes where aid=:aid and uid=:uid limit 1")
	Long audioDataVid(@Bind("uid") Long uid, @Bind("aid") Long aid);

	@SqlQuery("select *" //
			+ " from aqv_votes" //
			+ " where aid is null OR aid < 1")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioVoteEntriesForMigration();

	@SqlQuery("select vid from aqv_votes where " + " uid=:uid AND good=0 AND poor=0 AND bad=0")
	List<Long> undecidedVids(@Bind("uid") long uid);

	@Transaction
	default List<Long> pendingVids(long uid) {
		Map<Long, Float> rankings = voteRankingsByAid(MIN_VOTES_FILTER);
		List<Long> undecided = undecidedVids(uid);
		if (undecided.size() < NEW_ENTRIES_COUNT * 2) {
			scanForNewEntries(uid);
			undecided = undecidedVids(uid);
		}
		Iterator<Long> iter = undecided.iterator();
		while (iter.hasNext()) {
			Long vid = iter.next();
			AudioData audioData = audioDataInfoByVid(vid);
			Float ranking = rankings.get(audioData.getAid());
			ranking = (ranking == null ? 0 : ranking);
			if (!audioBytesInfoHasData(audioData.getAid()) || ranking < -2 || ranking > 1) {
				removeVoteEntry(uid, vid);
				iter.remove();
			}
		}
		return undecided;
	}

	default void scanForNewEntries(long uid) {
		AtomicInteger maxNewFiles = new AtomicInteger(NEW_ENTRIES_COUNT);
		Set<Long> already = new HashSet<>(audioDataAidsFor(uid));
		Map<Long, Float> rankings = voteRankingsByAid(MIN_VOTES_FILTER);
		List<AudioBytesInfo> entries = audioBytesInfo();
		entries.forEach(f -> {
			if (maxNewFiles.get() <= 0) {
				return;
			}
			if (already.contains(f.getAid())) {
				return;
			}
			Float ranking = rankings.get(f.getAid());
			if ((ranking == null ? 0 : ranking) < -2) {
				return;
			}
			addPendingEntry(uid, f.getAid(), f.getFile(), f.getTxt());
			maxNewFiles.decrementAndGet();
		});
	}

	@Transaction
	@SqlUpdate("insert into aqv_votes (uid, aid)" + " select :uid, :aid from (select 1) b" + " where not exists"
			+ " (select 1 from aqv_votes where uid=:uid AND aid=:aid);" + " update aqv_votes" //
			+ " set txt=:text, file=:file" //
			+ " where uid=:uid AND aid=:aid")
	void addPendingEntry(@Bind("uid") long uid, @Bind("aid") Long aid, @Bind("file") String file,
			@Bind("text") String text);

	@Transaction
	default String newSessionId(long uid) {
		BigInteger no;
		String input = uid + "." + System.currentTimeMillis() + "." + new Random().nextLong();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] messageDigest = md.digest(input.getBytes());
			no = new BigInteger(1, messageDigest);
		} catch (NoSuchAlgorithmException e) {
			no = BigInteger.valueOf(new Random().nextLong()).multiply(BigInteger.valueOf(uid))
					.add(BigInteger.valueOf(new Random().nextLong()));
		}
		String sessionId = no.toString(Character.MAX_RADIX);
		insertSessionId(uid, sessionId);
		return sessionId;
	}

	@Transaction
	@SqlUpdate("insert into aqv_sessions (uid, session)" //
			+ " select :uid, :sessionId from (select 1) b" //
			+ " where not exists" //
			+ " (select 1 from aqv_sessions where uid=:uid AND session=:sessionId)")
	void insertSessionId(@Bind("uid") long uid, @Bind("sessionId") String sessionId);

	@SqlQuery("select count(*)>0 from aqv_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	boolean isSessionId(@Bind("uid") long uid, @Bind("sessionId") String sessionId);

	@SqlUpdate("delete from aqv_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	void deleteSessionId(@Bind("uid") long uid, @Bind("sessionId") String sessionId);

	class State {
		protected static HikariDataSource ds;
		protected static AudioQualityVoteDao dao;
		protected static String jdbcUrl;
		protected static Properties properties;
		protected static String user;
		protected static String password;

		protected static void loadPropertiesFile() {
			if (properties != null) {
				return;
			}
			properties = new Properties();
			File file = new File(Consts.DEFAULT_PROPERTIES_FILE);
			if (!file.exists()) {
				file = new File(Consts.ALT_PROPERTIES_FILE);
			}
			try (FileInputStream in = new FileInputStream(file.getAbsoluteFile())) {
				properties.load(in);
			} catch (IOException e) {
				properties = null;
				throw new IllegalStateException("Can't read " + file.getAbsolutePath(), e);
			}
			jdbcUrl = properties.getProperty("jdbc.url");
			user = properties.getProperty("jdbc.username");
			password = properties.getProperty("jdbc.password");
		}
	}

	@SqlQuery("select uid from aqv_users" //
			+ " where email=:email")
	long uidByEmail(@Bind("email") String email);

	@SqlUpdate("insert into aqv_users (oauth_provider, oauth_id, email)"
			+ " select :provider, :id, :email from (select 1) b" + " where not exists "
			+ " (select 1 from aqv_users where oauth_provider=:provider AND oauth_id=:id)")
	void addUser(@Bind("provider") String oauthProvider, @Bind("id") String oauthId, @Bind("email") String email);

	@SqlUpdate("update aqv_users set email=:email" //
			+ " where oauth_provider=:provider AND oauth_id=:id")
	void updateEmail(@Bind("provider") String oauthProvider, @Bind("id") String oauthId, @Bind("email") String email);

	@SqlUpdate("update aqv_votes" //
			+ " set bad=:bad, poor=:poor, good=:good" + " where vid=:vid and uid=:uid")
	void setVote(@Bind("uid") Long uid, @Bind("vid") Long vid, @Bind("bad") Integer bad, @Bind("poor") Integer poor,
			@Bind("good") Integer good);

	@SqlQuery("select vid from aqv_votes where uid=:uid order by file")
	List<Long> audioDataVidsFor(@Bind("uid") Long uid);

	@SqlQuery("select aid from aqv_votes where uid=:uid")
	List<Long> audioDataAidsFor(@Bind("uid") Long uid);

	@SqlQuery("select aid," //
			+ " sum(bad) bad, sum(poor) poor, sum(good) good," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," //
			+ " count(*) votes" //
			+ " from aqv_votes" //
			+ " where bad>0 OR poor>0 or good>0" //
			+ " group by aid order by ranking desc, aid")
	@RegisterBeanMapper(VoteResult.class)
	List<VoteResult> audioVoteResults();

	@SqlUpdate("delete from aqv_votes where vid=:vid AND uid=:uid")
	void removeVoteEntry(@Bind("uid") Long uid, @Bind("vid") Long vid);

	@SqlQuery("select count(*) from aqv_sessions where uid=:uid")
	int sessionCount(@Bind("uid") Long uid);

	@SqlUpdate("delete from aqv_sessions where uid=:uid order by last_seen limit :limit")
	void deleteOldestSessions(@Bind("uid") Long uid, @Bind("limit") int limit);

	@SqlUpdate("update aqv_users set last_login=NOW(), modified=modified where uid=:uid")
	void updateLastLogin(@Bind("uid") Long uid);

	@SqlQuery("select aid," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," + " count(*) votes" //
			+ " from aqv_votes" //
			+ " where" //
			+ " (bad>0 OR poor>0 or good>0)" //
			+ " group by file" //
			+ " having votes >= :minVotes" + " order by rand()")
	@KeyColumn("aid")
	@ValueColumn("ranking")
	Map<Long, Float> voteRankingsByAid(@Bind("minVotes") int minVotes);

	@SqlQuery("select file," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," + " count(*) votes" //
			+ " from aqv_votes" //
			+ " where" //
			+ " (bad>0 OR poor>0 or good>0)" //
			+ " group by file" //
			+ " having votes >= :minVotes" + " AND" + " ranking >= :minRanking")
	@KeyColumn("file")
	@ValueColumn("ranking")
	Map<String, Float> voteRankingsByFile(@Bind("minVotes") int minVotes, @Bind("minRanking") double minRanking);

	@SqlQuery("select count(*) from aqv_votes where uid=:uid AND good=0 AND poor=0 AND bad=0")
	int userPendingVoteCount(@Bind("uid") Long uid);

	@SqlQuery("select count(*) from aqv_votes where uid=:uid AND (good=1 OR poor=1 OR bad=1)")
	int userCompletedVoteCount(@Bind("uid") Long uid);

	@SqlQuery("select uid, count(*) voted, max(modified) m from aqv_votes" //
			+ " where (good!=0 OR poor!=0 OR bad!=0)" //
			+ " group by uid" //
			+ " order by voted desc, m desc" //
			+ " limit :limit")
	List<Long> topUsersByVoteCounts(@Bind("limit") int limit);

	@SqlQuery("select count(*) from aqv_audio")
	long audioTrackCount();

	@SqlUpdate("delete from aqv_users where uid=:uid and :uid!=0;"
			+ " delete from aqv_votes where uid=:uid and :uid!=0;"
			+ " delete from aqv_audio where uid=:uid and :uid!=0;"
			+ " delete from aqv_sessions where uid=:uid and :uid!=0;")
	void deleteUserById(@Bind("uid") Long uid);

	@SqlUpdate("update aqv_sessions set last_seen=NOW() where uid=:uid AND session=:session")
	void updateLastSeen(@Bind("uid") Long uid, @Bind("session") String sessionId);

	@SqlUpdate("delete from aqv_sessions where last_seen < NOW() - INTERVAL 1 WEEK")
	void deleteOldSessions();

	default List<String> availableTexts() {
		return userTexts(0l);
	}

	@SqlQuery("select count(*)>0 from aqv_audio where uid=0 AND txt=:text")
	boolean isValidText(@Bind("text") String text);

	@SqlQuery("select file from aqv_audio where uid=0 AND txt=:text")
	String fileForText(@Bind("text") String text);

	@SqlQuery("select aid from aqv_audio where uid=0 AND txt=:text")
	Long aidForText(@Bind("text") String text);

	@SqlQuery("select distinct txt from aqv_audio where uid=:uid")
	List<String> userTexts(@Bind("uid") Long uid);

	@SqlQuery("select count(*)>0 from aqv_audio where uid=:uid AND file=:file and data is not null")
	boolean audioBytesInfoExistsFor(@Bind("uid") long uid, @Bind("file") String file);

	@SqlQuery("select count(*)>0 from aqv_audio where aid=:aid and data is not null")
	boolean audioBytesInfoHasData(@Bind("aid") long aid);

	@SqlUpdate("update aqv_votes set aid=:aid where file=:file")
	void setAudioIdForMatchingVotes(@Bind("aid") long aid, @Bind("file") String file);

	@SqlQuery("select aid from aqv_audio where file=:file order by uid limit 1")
	Long getAidForFile(@Bind("file") String file);

	@SqlQuery("select mime from aqv_audio where aid=:aid")
	String audioBytesMime(@Bind("aid") Long aid);
}
