package com.cherokeelessons.audio.quality.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Proves the multi-statement vote path executes through the URL the
 * application assembles from the DB_* environment keys:
 * <code>addPendingEntry</code> issues two statements in one @SqlUpdate
 * string, which the MariaDB driver only executes when the JDBC URL carries
 * <code>allowMultiQueries=true</code> — the passing assertion through the
 * assembled URL is the proof, with a without-the-parameter contrast showing
 * the same statement errors.
 */
public class AddPendingEntryTest {

	private static String url;
	private static String user;
	private static String password;
	private static final String TABLE = "AddPendingEntryTest";
	private static Connection conn;

	@BeforeClass
	public static void setup() throws SQLException {
		resolveFromEnv();
		conn = DriverManager.getConnection(url, user, password);
		try (Statement st = conn.createStatement()) {
			st.execute("create table if not exists " + TABLE + "_audio ("
					+ " aid bigint auto_increment primary key,"
					+ " uid bigint unsigned,"
					+ " file varchar(255),"
					+ " txt text,"
					+ " mime text)");
			st.execute("create table if not exists " + TABLE + "_votes ("
					+ " vid bigint auto_increment primary key,"
					+ " uid bigint unsigned,"
					+ " aid bigint unsigned,"
					+ " file varchar(255),"
					+ " txt text,"
					+ " good int default 0, poor int default 0, bad int default 0)");
			st.execute("delete from " + TABLE + "_votes");
		}
	}

	private static void resolveFromEnv() {
		Map<String, String> env = new HashMap<>(System.getenv());
		if (!env.keySet().containsAll(Arrays.asList("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER"))
				|| (env.get("DB_PASSWORD") == null && env.get("DB_PASSWORD_FILE") == null)) {
			assumeTrue("DB_* environment keys not set — see SOP local-run procedure", false);
		}
		AudioQualityVoteDao.State.resolveConfig(env);
		url = AudioQualityVoteDao.State.jdbcUrl;
		user = AudioQualityVoteDao.State.user;
		password = AudioQualityVoteDao.State.password;
	}

	@After
	public void cleanVotes() throws SQLException {
		try (Statement st = conn.createStatement()) {
			st.execute("delete from " + TABLE + "_votes");
		}
	}

	private AudioQualityVoteDao dao() {
		return Jdbi.create(url, user, password).installPlugin(new SqlObjectPlugin()) //
				.onDemand(AudioQualityVoteDao.class);
	}

	@Test
	public void multiStatementUpdateCommitsBothStatementsThroughAssembledUrl() throws SQLException {
		AudioQualityVoteDao dao = dao();
		dao.addPendingEntry(TABLE, 42L, 7L, "audio/clip1.mp3", "hello");
		try (PreparedStatement ps = conn.prepareStatement(
				"select txt, file from " + TABLE + "_votes where uid=? AND aid=?")) {
			ps.setLong(1, 42L);
			ps.setLong(2, 7L);
			try (ResultSet rs = ps.executeQuery()) {
				assertTrue("pending row inserted by the second statement", rs.next());
				assertEquals("hello", rs.getString(1));
				assertEquals("audio/clip1.mp3", rs.getString(2));
			}
		}
	}

	@Test
	public void sameStatementWithoutAllowMultiQueriesErrors() throws SQLException {
		// the parameter is load-bearing: strip it and the identical
		// two-statement update errors rather than executing
		String urlNoParam = url.replace("?allowMultiQueries=true", "");
		try (Connection c = DriverManager.getConnection(urlNoParam, user, password)) {
			try (Statement st = c.createStatement()) {
				st.execute("insert into " + TABLE + "_votes (uid, aid) select 42, 7 from (select 1) b"
						+ " where not exists (select 1 from " + TABLE + "_votes where uid=42 AND aid=7);"
						+ " update " + TABLE + "_votes set txt='x' where uid=42 AND aid=7");
				fail("multi-statement SQL must error without allowMultiQueries=true");
			} catch (SQLException expected) {
				// the driver refuses the multi-statement string
			}
		}
	}

	private static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}
}
