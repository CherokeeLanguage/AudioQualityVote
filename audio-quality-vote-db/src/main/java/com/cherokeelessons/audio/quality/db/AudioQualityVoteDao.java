package com.cherokeelessons.audio.quality.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlScript;

public interface AudioQualityVoteDao {

	static AudioQualityVoteDao onDemand() {
		if (State.dao != null) {
			return State.dao;
		}
		State.loadPropertiesFile();
		Connection connection;
		try {
			connection = DriverManager.getConnection(State.jdbcUrl, State.user, State.password);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		Jdbi jdbi = Jdbi.create(connection);
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
	//
	@SqlScript("create table if not exists aqv_audio" //
			+ " (aid serial," //
			+ " file varchar(254)," //
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists file on aqv_audio(file(8))")
	//
	@SqlScript("create table if not exists aqv_votes" //
			+ " (vid serial," //
			+ " uid bigint unsigned," + " aid bigint unsigned," //
			+ " up int default 0," + " neutral int default 0," + " down int default 0,"
			+ " modified datetime on update NOW()," //
			+ " created datetime default NOW())") //
	@SqlScript("create index if not exists file on aqv_votes(file(8))")
	@SqlScript("create index if not exists modified on aqv_votes(modified)")
	@SqlScript("create index if not exists created on aqv_votes(created)")
	void init();

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
				throw new IllegalStateException("Can't read " + file.getAbsolutePath(), e);
			}
			jdbcUrl = properties.getProperty("jdbc.url");
			user = properties.getProperty("jdbc.username");
			password = properties.getProperty("jdbc.password");
		}
	}
}
