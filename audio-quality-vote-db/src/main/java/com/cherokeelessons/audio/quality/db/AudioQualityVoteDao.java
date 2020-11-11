package com.cherokeelessons.audio.quality.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import com.cherokeelessons.audio.quality.shared.AudioData;

public interface AudioQualityVoteDao {

	static AudioQualityVoteDao onDemand() {
		if (State.dao != null) {
			return State.dao;
		}
		State.loadPropertiesFile();
		
		MariaDbPoolDataSource pool = new MariaDbPoolDataSource();
		try {
			pool.setUrl(State.jdbcUrl);
			pool.setUser(State.user);
			pool.setPassword(State.password);
			pool.setMaxIdleTime(60);
			pool.setMaxPoolSize(4);
			pool.setMinPoolSize(1);
		} catch (SQLException e) {
			pool.close();
			throw new IllegalStateException(e);
		}		
		Jdbi jdbi = Jdbi.create(pool);
		jdbi.installPlugin(new SqlObjectPlugin());
		AudioQualityVoteDao onDemand = jdbi.onDemand(AudioQualityVoteDao.class);
		onDemand.init();
		return onDemand;
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
			+ " txt text,"
			+ " good int default 0," //
			+ " poor int default 0," //
			+ " bad int default 0,"
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists uid on aqv_votes(uid)")
	@SqlScript("create index if not exists file on aqv_votes(file(32))")
	@SqlScript("create index if not exists txt on aqv_votes(txt(8))")
	@SqlScript("create index if not exists modified on aqv_votes(modified)")
	@SqlScript("create index if not exists created on aqv_votes(created)")
	void init();
	
	@SqlQuery("select *, file as audioFile, txt as `text`" //
			+ " from aqv_votes where" //
			+ " vid=:vid")
	@RegisterBeanMapper(AudioData.class)
	AudioData audioData(@Bind("vid")long vid);
	
	@SqlQuery("select vid from aqv_votes where "
			+ " uid=:uid AND good=0 AND poor=0 AND bad=0")
	List<Integer> undecided(@Bind("uid")long uid);
	
	@Transaction
	default List<Integer> pending(long uid) {
		List<Integer> undecided = undecided(uid);
		if (undecided.isEmpty()) {
			File parentFolder = AudioQualityVoteFiles.getFolder().getAbsoluteFile();
			List<AudioData> files = AudioQualityVoteFiles.getAudioData();
			files.forEach(f->{
				String relative = f.getAudioFile().substring(parentFolder.getPath().length());
				addPendingFile(uid, relative, f.getText());
			});
			return undecided(uid);
		}
		return undecided;
	}

	@Transaction
	@SqlUpdate("insert into aqv_votes (uid, file)"
			+ " select :uid, :file from (select 1) b"
			+ " where not exists"
			+ " (select * from aqv_votes where uid=:uid AND file=:file);"
			+ " update aqv_votes" //
			+ " set txt=:text" //
			+ " where uid=:uid AND file=:file AND (txt!=:text OR txt is null)")	
	void addPendingFile(@Bind("uid")long uid, @Bind("file")String relative, @Bind("text")String text);

	@Transaction	
	default String newSessionId(long uid) {
		BigInteger no;
		String input = uid + "." +System.currentTimeMillis() + "." + new Random().nextLong();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] messageDigest = md.digest(input.getBytes());
			no = new BigInteger(1, messageDigest);
		} catch (NoSuchAlgorithmException e) {
			no = BigInteger.valueOf(new Random().nextLong()).multiply(BigInteger.valueOf(uid)).add(BigInteger.valueOf(new Random().nextLong()));
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
	void insertSessionId(@Bind("uid")long uid, @Bind("sessionId")String sessionId);
	
	@SqlQuery("select count(*)>0 from aqv_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	boolean isSessionId(@Bind("uid")long uid, @Bind("sessionId")String sessionId);
	
	@SqlUpdate("delete from aqv_sessions" //
			+ " where uid=:uid AND session=:sessionId")
	void deleteSessionId(@Bind("uid")long uid, @Bind("sessionId")String sessionId);

	class State {
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
				properties=null;
				throw new IllegalStateException("Can't read " + file.getAbsolutePath(), e);
			}
			jdbcUrl = properties.getProperty("jdbc.url");
			user = properties.getProperty("jdbc.username");
			password = properties.getProperty("jdbc.password");
		}
	}

	@SqlQuery("select uid from aqv_users" //
			+ " where email=:email")
	long uid(@Bind("email")String email);
	
	@SqlUpdate("insert into aqv_users (oauth_provider, oauth_id, email)"
			+ " select :provider, :id, :email from (select 1) b"
			+ " where not exists "
			+ " (select 1 from aqv_users where oauth_provider=:provider AND oauth_id=:id)")
	void addUser(@Bind("provider")String oauthProvider, @Bind("id")String oauthId, @Bind("email")String email);
	
	@SqlUpdate("update aqv_users set email=:email" //
			+ " where oauth_provider=:provider AND oauth_id=:id")
	void updateEmail(@Bind("provider")String oauthProvider, @Bind("id")String oauthId, @Bind("email")String email);

	@SqlUpdate("update aqv_votes" //
			+ " set bad=:bad, poor=:poor, good=:good"
			+ " where vid=:vid and uid=:uid")
	void setVote(@Bind("uid")Long uid, @Bind("vid")Long vid, @Bind("bad")Integer bad, @Bind("poor")Integer poor, @Bind("good")Integer good);
}
