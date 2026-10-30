package com.cherokeelessons.audio.quality.db;

import static org.junit.Assert.assertEquals;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Exercises <code>uidByEmail</code>'s no-results contract directly against the
 * local dev MariaDB (docker container aqv-mysql). The live deploy threw
 * "SQL method returns primitive long, but statement returned no results"
 * from this exact method on a first-ever email.
 */
public class UidByEmailTest {

	private static final String URL = "jdbc:mysql://127.0.0.1:3306/audiovote";
	private static final String USER = "audiovote";
	private static final String PASS = "audiovote_pw";
	private static final String TABLE = "UidByEmailTest_users";
	private static Connection conn;

	@BeforeClass
	public static void setup() throws SQLException {
		conn = DriverManager.getConnection(URL, USER, PASS);
		try (Statement st = conn.createStatement()) {
			st.execute("create table if not exists " + TABLE + " ("
					+ " uid bigint auto_increment primary key,"
					+ " email varchar(255),"
					+ " oauth_provider varchar(64),"
					+ " oauth_id varchar(64))");
			st.execute("delete from " + TABLE);
		}
	}

	@AfterClass
	public static void teardown() throws SQLException {
		if (conn != null) {
			try (Statement st = conn.createStatement()) {
				st.execute("drop table if exists " + TABLE);
			}
			conn.close();
		}
	}

	private AudioQualityVoteDao dao() {
		return Jdbi.create(URL, USER, PASS).installPlugin(new SqlObjectPlugin()) //
				.onDemand(AudioQualityVoteDao.class);
	}

	private void insert(String email, String provider, String id) throws SQLException {
		try (PreparedStatement ps = conn
				.prepareStatement("insert into " + TABLE + " (email, oauth_provider, oauth_id) values (?,?,?)")) {
			ps.setString(1, email);
			ps.setString(2, provider);
			ps.setString(3, id);
			ps.execute();
		}
	}

	@Test
	public void unknownEmailReturnsZeroNotExplosion() {
		assertEquals(0L, dao().uidByEmail("UidByEmailTest", "nobody-first-ever@example.invalid"));
	}

	@Test
	public void knownEmailReturnsOldestUid() throws SQLException {
		// two rows on the same email — email-based identity must ride the
		// oldest row, the selection the former "order by uid asc limit 1" gave
		insert("twice@example.invalid", "Google", "g-1");
		Thread.yield();
		insert("twice@example.invalid", "GitHub", "gh-9");
		assertEquals(
				Long.parseLong(selectOldestUid("twice@example.invalid")),
				dao().uidByEmail("UidByEmailTest", "twice@example.invalid"));
	}

	private String selectOldestUid(String email) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("select min(uid) from " + TABLE + " where email=?")) {
			ps.setString(1, email);
			try (java.sql.ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString(1);
			}
		}
	}
}