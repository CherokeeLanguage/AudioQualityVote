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
import java.util.Collections;
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
import org.jdbi.v3.sqlobject.customizer.Define;
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

	public static final int NEW_ENTRIES_COUNT = 5;
	int MIN_VOTES_FILTER = 3;

	static AudioQualityVoteDao onDemand(String tablePrefix) {
		synchronized (AudioQualityVoteDao.State.class) {
			if (State.dao!=null) {
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
			onDemand.init(tablePrefix);
			State.dao = onDemand;
			return onDemand;
		}
	}

	static void close() {
		if (State.ds != null) {
			State.ds.close();
		}
	}

	@SqlScript("alter database character set = 'utf8mb4'")
	@SqlScript("create table if not exists <table>_users" //
			+ " (uid serial, oauth_provider varchar(254)," //
			+ " oauth_id varchar(254), email varchar(254)," //
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW()" //
			+ ")")
	@SqlScript("create index if not exists oauth_provider on <table>_users(oauth_provider(4))")
	@SqlScript("create index if not exists oauth_id on <table>_users(oauth_id(4))")
	@SqlScript("create index if not exists email on <table>_users(email(4))")
	@SqlScript("alter table <table>_users add column if not exists last_login datetime")

	@SqlScript("create table if not exists <table>_sessions" //
			+ " (sid serial, uid bigint unsigned, session varchar(254)," //
			+ " last_seen datetime default NOW() on update NOW()," //
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW()" //
			+ ")")
	@SqlScript("create index if not exists uid on <table>_sessions(uid)")
	@SqlScript("create index if not exists session on <table>_sessions(session(16))")
	@SqlScript("create index if not exists last_seen on <table>_sessions(last_seen)")

	//
	@SqlScript("create table if not exists <table>_votes" //
			+ " (vid serial," //
			+ " uid bigint unsigned," //
			+ " file varchar(254)," //
			+ " txt text," + " good int default 0," //
			+ " poor int default 0," //
			+ " bad int default 0," + " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists uid on <table>_votes(uid)")
	@SqlScript("create index if not exists file on <table>_votes(file(32))")
	@SqlScript("create index if not exists txt on <table>_votes(txt(8))")
	@SqlScript("create index if not exists modified on <table>_votes(modified)")
	@SqlScript("create index if not exists created on <table>_votes(created)")

	@SqlScript("alter table <table>_votes add column if not exists aid bigint unsigned")
	@SqlScript("create index if not exists aid on <table>_votes(aid)")

	@SqlScript("create table if not exists <table>_audio" //
			+ " (aid serial," //
			+ " uid bigint unsigned," //
			+ " file text," //
			+ " txt text," + " mime text," + " data longblob," + " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists uid on <table>_audio(uid)")
	@SqlScript("create index if not exists file on <table>_audio(file(32))")
	@SqlScript("create index if not exists txt on <table>_audio(txt(8))")
	@SqlScript("create index if not exists mime on <table>_audio(mime(16))")
	@SqlScript("create index if not exists modified on <table>_audio(modified)")
	@SqlScript("create index if not exists created on <table>_audio(created)")
	void init(@Define("table")String tablePrefix);

	default void audioBytesStream(String tablePrefix, long aid, OutputStream os) throws IOException {
		AudioBytesObject data = audioBytesObject(tablePrefix, aid);
		if (data == null || data.getData() == null) {
			data = new AudioBytesObject();
			data.setData(new byte[0]);
		}
		try (InputStream bs = new ByteArrayInputStream(data.getData())) {
			IOUtils.copy(bs, os);
		}
	}

	@SqlQuery("select data from <table>_audio where aid=:aid")
	byte[] audioBytes(@Define("table")String tablePrefix, @Bind("aid") long aid);

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
	@SqlQuery("select data from <table>_audio where aid=:aid")
	AudioBytesObject audioBytesObject(@Define("table")String tablePrefix, @Bind("aid") long aid);

	@SqlUpdate("delete from <table>_audio where aid=:aid")
	void deleteAudioBytes(@Define("table")String tablePrefix, @Bind("aid") long aid);

	@SqlUpdate("delete from <table>_audio where uid=:uid")
	void deleteAudioBytesByUid(@Define("table")String tablePrefix, @Bind("uid") long uid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from <table>_audio where aid=:aid")
	@RegisterBeanMapper(AudioBytesInfo.class)
	AudioBytesInfo audioBytesInfo(@Define("table")String tablePrefix, @Bind("aid") long aid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from <table>_audio where uid=:uid")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioBytesInfoFor(@Define("table")String tablePrefix, @Bind("uid") long uid);

	@SqlQuery("select aid, uid, file, txt, mime" //
			+ " from <table>_audio")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioBytesInfo(@Define("table")String tablePrefix);

	@Transaction
	default long addAudioBytesInfo(String tablePrefix, AudioBytesInfo info) {
		deleteAudioBytesInfo(tablePrefix, info);
		return insertAudioBytesInfo(tablePrefix, info);
	}

	@SqlUpdate("insert into <table>_audio (uid, file, txt, mime)" //
			+ " select :uid, :file, :txt, :mime from (select 1) a" //
			+ " where not exists" //
			+ " (select 1 from <table>_audio where file=:file)")
	@GetGeneratedKeys
	long insertAudioBytesInfo(@Define("table")String tablePrefix, @BindBean AudioBytesInfo info);

	@SqlUpdate("delete from <table>_audio where uid=:uid and file=:file")
	void deleteAudioBytesInfo(@Define("table")String tablePrefix, @BindBean AudioBytesInfo info);

	@SqlUpdate("update <table>_audio set data=:data where aid=:aid")
	void setAudioBytesData(@Define("table")String tablePrefix, @Bind("aid") long aid, @Bind("data") byte[] data);

	default void setAudioBytesData(String tablePrefix, long aid, InputStream data) {
		useHandle(h -> {
			h.createUpdate("update <table>_audio set data=:data where aid=:aid") //
			.define("table", tablePrefix) //
					.bind("data", new ServletInputStreamArgument(data)) //
					.bind("aid", aid) //
					.execute();
		});
	}

	default void setAudioBytesData(String tablePrefix, long aid, File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			setAudioBytesData(tablePrefix, aid, fis);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SqlQuery("select *" //
			+ " from <table>_audio where" //
			+ " aid=:aid")
	@RegisterBeanMapper(AudioData.class)
	AudioData audioDataInfoByAid(@Define("table")String tablePrefix, @Bind("aid") long aid);

	@SqlQuery("select *" //
			+ " from <table>_votes where" //
			+ " vid=:vid")
	@RegisterBeanMapper(AudioData.class)
	AudioData audioDataInfoByVid(@Define("table")String tablePrefix, @Bind("vid") long vid);

	@SqlQuery("select vid from <table>_votes where aid=:aid and uid=:uid limit 1")
	Long audioDataVid(@Define("table")String tablePrefix, @Bind("uid") Long uid, @Bind("aid") Long aid);

	@SqlQuery("select *" //
			+ " from <table>_votes" //
			+ " where aid is null OR aid < 1")
	@RegisterBeanMapper(AudioBytesInfo.class)
	List<AudioBytesInfo> audioVoteEntriesForMigration(@Define("table")String tablePrefix);

	@SqlQuery("select vid from <table>_votes where " //
			+ " uid=:uid AND good=0 AND poor=0 AND bad=0")
	List<Long> undecidedVids(@Define("table")String tablePrefix, @Bind("uid") long uid);

	@Transaction
	default List<Long> pendingVids(String tablePrefix, long uid) {
		Map<Long, Float> rankings = voteRankingsByAid(tablePrefix, MIN_VOTES_FILTER);
		List<Long> undecided = undecidedVids(tablePrefix, uid);
		if (undecided.size()<NEW_ENTRIES_COUNT*2) {
			scanForNewEntries(tablePrefix, uid);
			undecided = undecidedVids(tablePrefix, uid);
		}
		Iterator<Long> iter = undecided.iterator();
		while (iter.hasNext()) {
			Long vid = iter.next();
			AudioData audioData = audioDataInfoByVid(tablePrefix, vid);
			Long aid = audioData.getAid();
			float ranking = rankings.containsKey(aid) ? rankings.get(aid) : 0;
			if (!audioBytesInfoHasData(tablePrefix, aid) || ranking < -1 || ranking > 1) {
				removeVoteEntry(tablePrefix, uid, vid);
				iter.remove();
			}
		}
		return undecided;
	}

	default void scanForNewEntries(String tablePrefix, long uid) {
		AtomicInteger maxNewFiles = new AtomicInteger(NEW_ENTRIES_COUNT);
		Set<Long> already = new HashSet<>(audioDataAidsFor(tablePrefix, uid));
		Map<Long, Float> rankings = voteRankingsByAid(tablePrefix, MIN_VOTES_FILTER);
		List<AudioBytesInfo> entries = audioBytesInfo(tablePrefix);
		Collections.shuffle(entries);
		for (AudioBytesInfo f: entries) {
			final long aid = f.getAid();
			if (already.contains(aid)) {
				continue;
			}
			float ranking = rankings.containsKey(aid) ? rankings.get(aid) : 0;
			if (ranking < -1 || ranking > 1) {
				continue;
			}
			addPendingEntry(tablePrefix, uid, aid, f.getFile(), f.getTxt());
			if (maxNewFiles.decrementAndGet() <= 0) {
				break;
			}
		}
	}

	@Transaction
	@SqlUpdate("insert into <table>_votes (uid, aid)" + " select :uid, :aid from (select 1) b" + " where not exists"
			+ " (select 1 from <table>_votes where uid=:uid AND aid=:aid);" + " update <table>_votes" //
			+ " set txt=:text, file=:file" //
			+ " where uid=:uid AND aid=:aid")
	void addPendingEntry(@Define("table")String tablePrefix, //
			@Bind("uid") long uid, @Bind("aid") Long aid, @Bind("file") String file,
			@Bind("text") String text);

	@Transaction
	default String newSessionId(String tablePrefix, long uid) {
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
		insertSessionId(tablePrefix, uid, sessionId);
		return sessionId;
	}

	@Transaction
	@SqlUpdate("insert into <table>_sessions (uid, session)" //
			+ " select :uid, :sessionId from (select 1) b" //
			+ " where not exists" //
			+ " (select 1 from <table>_sessions where uid=:uid AND session=:sessionId)")
	void insertSessionId(@Define("table")String tablePrefix, @Bind("uid") long uid, @Bind("sessionId") String sessionId);

	@SqlQuery("select count(*)>0 from <table>_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	boolean isSessionId(@Define("table")String tablePrefix, @Bind("uid") long uid, @Bind("sessionId") String sessionId);

	@SqlUpdate("delete from <table>_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	void deleteSessionId(@Define("table")String tablePrefix, @Bind("uid") long uid, @Bind("sessionId") String sessionId);

	class State {
		protected static String tablePrefix;
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
			File file = AppPathConfig.PROPERTIES_FILE;
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

	@SqlQuery("select uid from <table>_users" //
			+ " where email=:email")
	long uidByEmail(@Define("table")String tablePrefix, @Bind("email") String email);

	@SqlUpdate("insert into <table>_users (oauth_provider, oauth_id, email)"
			+ " select :provider, :id, :email from (select 1) b" + " where not exists "
			+ " (select 1 from <table>_users where oauth_provider=:provider AND oauth_id=:id)")
	void addUser(@Define("table")String tablePrefix, @Bind("provider") String oauthProvider, @Bind("id") String oauthId, @Bind("email") String email);

	@SqlUpdate("update <table>_users set email=:email" //
			+ " where oauth_provider=:provider AND oauth_id=:id")
	void updateEmail(@Define("table")String tablePrefix, @Bind("provider") String oauthProvider, @Bind("id") String oauthId, @Bind("email") String email);

	@SqlUpdate("update <table>_votes" //
			+ " set bad=:bad, poor=:poor, good=:good" + " where vid=:vid and uid=:uid")
	void setVote(@Define("table")String tablePrefix, @Bind("uid") Long uid, @Bind("vid") Long vid, @Bind("bad") Integer bad, @Bind("poor") Integer poor,
			@Bind("good") Integer good);

	@SqlQuery("select vid from <table>_votes where uid=:uid order by file")
	List<Long> audioDataVidsFor(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select aid from <table>_votes where uid=:uid")
	List<Long> audioDataAidsFor(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select aid," //
			+ " sum(bad) bad, sum(poor) poor, sum(good) good," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," //
			+ " count(*) votes" //
			+ " from <table>_votes" //
			+ " where bad>0 OR poor>0 or good>0" //
			+ " group by aid order by ranking desc, aid")
	@RegisterBeanMapper(VoteResult.class)
	List<VoteResult> audioVoteResults_old(@Define("table")String tablePrefix);
	
	@SqlQuery("select aid," //
			+ " sum(bad) bad, 0 poor, sum(good) good," //
			+ " avg(good) - avg(bad) ranking," //
			+ " count(*) votes" //
			+ " from <table>_votes" //
			+ " where bad>0 OR poor>0 or good>0" //
			+ " group by aid order by ranking desc, aid")
	@RegisterBeanMapper(VoteResult.class)
	List<VoteResult> audioVoteResults(@Define("table")String tablePrefix);

	@SqlUpdate("delete from <table>_votes where vid=:vid AND uid=:uid")
	void removeVoteEntry(@Define("table")String tablePrefix, @Bind("uid") Long uid, @Bind("vid") Long vid);

	@SqlQuery("select count(*) from <table>_sessions where uid=:uid")
	int sessionCount(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlUpdate("delete from <table>_sessions where uid=:uid order by last_seen limit :limit")
	void deleteOldestSessions(@Define("table")String tablePrefix, @Bind("uid") Long uid, @Bind("limit") int limit);

	@SqlUpdate("update <table>_users set last_login=NOW(), modified=modified where uid=:uid")
	void updateLastLogin(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select aid," //
			+ " avg(good) - avg(bad) ranking," //
			+ " count(*) votes" //
			+ " from <table>_votes" //
			+ " where" //
			+ " (bad>0 or good>0)"
			+ " group by aid" //
			+ " having votes >= :minVotes")
	@KeyColumn("aid")
	@ValueColumn("ranking")
	Map<Long, Float> voteRankingsByAid(@Define("table")String tablePrefix, @Bind("minVotes") int minVotes);
	
	@SqlQuery("select aid," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," + " count(*) votes" //
			+ " from <table>_votes" //
			+ " where" //
			+ " (bad>0 OR poor>0 or good>0)" //
			+ " group by file" //
			+ " having votes >= :minVotes" + " order by rand()")
	@KeyColumn("aid")
	@ValueColumn("ranking")
	Map<Long, Float> voteRankingsByAid_old(@Define("table")String tablePrefix, @Bind("minVotes") int minVotes);	

	@SqlQuery("select file," //
			+ " avg(good) - (avg(bad)*2+avg(poor)) ranking," + " count(*) votes" //
			+ " from <table>_votes" //
			+ " where" //
			+ " (bad>0 OR poor>0 or good>0)" //
			+ " group by file" //
			+ " having votes >= :minVotes" + " AND" + " ranking >= :minRanking")
	@KeyColumn("file")
	@ValueColumn("ranking")
	Map<String, Float> voteRankingsByFile(@Define("table")String tablePrefix, @Bind("minVotes") int minVotes, @Bind("minRanking") double minRanking);

	@SqlQuery("select count(*) from <table>_votes where uid=:uid AND good=0 AND poor=0 AND bad=0")
	int userPendingVoteCount(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select count(*) from <table>_votes where uid=:uid AND (good=1 OR poor=1 OR bad=1)")
	int userCompletedVoteCount(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select uid, count(*) voted, max(modified) m from <table>_votes" //
			+ " where (good!=0 OR poor!=0 OR bad!=0)" //
			+ " group by uid" //
			+ " order by voted desc, m desc" //
			+ " limit :limit")
	List<Long> topUsersByVoteCounts(@Define("table")String tablePrefix, @Bind("limit") int limit);

	@SqlQuery("select count(*) from <table>_audio")
	long audioTrackCount();

	@SqlUpdate("delete from <table>_users where uid=:uid and :uid!=0;"
			+ " delete from <table>_votes where uid=:uid and :uid!=0;"
			+ " delete from <table>_audio where uid=:uid and :uid!=0;"
			+ " delete from <table>_sessions where uid=:uid and :uid!=0;")
	void deleteUserById(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlUpdate("update <table>_sessions set last_seen=NOW() where uid=:uid AND session=:session")
	void updateLastSeen(@Define("table")String tablePrefix, @Bind("uid") Long uid, @Bind("session") String sessionId);

	@SqlUpdate("delete from <table>_sessions where last_seen < NOW() - INTERVAL 1 WEEK")
	void deleteOldSessions(@Define("table")String tablePrefix);

	default List<String> availableTexts(String tablePrefix) {
		return userTexts(tablePrefix, 0l);
	}

	@SqlQuery("select count(*)>0 from <table>_audio where uid=0 AND txt=:text")
	boolean isValidText(@Define("table")String tablePrefix, @Bind("text") String text);

	@SqlQuery("select file from <table>_audio where uid=0 AND txt=:text")
	String fileForText(@Define("table")String tablePrefix, @Bind("text") String text);

	@SqlQuery("select aid from <table>_audio where uid=0 AND txt=:text")
	Long aidForText(@Define("table")String tablePrefix, @Bind("text") String text);

	@SqlQuery("select distinct txt from <table>_audio where uid=:uid")
	List<String> userTexts(@Define("table")String tablePrefix, @Bind("uid") Long uid);

	@SqlQuery("select count(*)>0 from <table>_audio where uid=:uid AND file=:file and data is not null")
	boolean audioBytesInfoExistsFor(@Define("table")String tablePrefix, @Bind("uid") long uid, @Bind("file") String file);

	@SqlQuery("select count(*)>0 from <table>_audio where aid=:aid and data is not null")
	boolean audioBytesInfoHasData(@Define("table")String tablePrefix, @Bind("aid") long aid);

	@SqlUpdate("update <table>_votes set aid=:aid where file=:file")
	void setAudioIdForMatchingVotes(@Define("table")String tablePrefix, @Bind("aid") long aid, @Bind("file") String file);

	@SqlQuery("select aid from <table>_audio where file=:file order by uid limit 1")
	Long getAidForFile(@Define("table")String tablePrefix, @Bind("file") String file);

	@SqlQuery("select mime from <table>_audio where aid=:aid")
	String audioBytesMime(@Define("table")String tablePrefix, @Bind("aid") Long aid);
}
