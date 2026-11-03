package com.cherokeelessons.audio.quality.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Exercises {@link AudioQualityVoteDao.State}'s environment-based configuration
 * resolution: required-key enforcement, password-form exclusivity
 * (DB_PASSWORD vs DB_PASSWORD_FILE), secret-file trailing-newline stripping,
 * and the assembled JDBC URL.
 */
public class StateConfigTest {

	private static Map<String, String> env(String... kv) {
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}

	@Test
	public void bothPasswordFormsIsError() throws Exception {
		try (TempSecretFile secret = new TempSecretFile("secret\n")) {
			Map<String, String> env = env("DB_HOST", "mariadb", "DB_PORT", "3306", "DB_NAME", "audiovote",
					"DB_USER", "audiovote", "DB_PASSWORD", "pw", "DB_PASSWORD_FILE", secret.path);
			try {
				AudioQualityVoteDao.State.resolveConfig(env);
				fail("both password forms set must be a configuration error");
			} catch (IllegalStateException expected) {
				// both $DB_PASSWORD and $DB_PASSWORD_FILE are exclusive
			}
		}
	}

	@Test
	public void neitherPasswordFormIsError() {
		Map<String, String> env = env("DB_HOST", "mariadb", "DB_PORT", "3306", "DB_NAME", "audiovote",
				"DB_USER", "audiovote");
		try {
			AudioQualityVoteDao.State.resolveConfig(env);
			fail("neither password form set must be a configuration error");
		} catch (IllegalStateException expected) {
			// no password source
		}
	}

	@Test
	public void missingRequiredKeyIsError() {
		// DB_HOST missing
		Map<String, String> env = env("DB_PORT", "3306", "DB_NAME", "audiovote", "DB_USER", "audiovote",
				"DB_PASSWORD", "pw");
		try {
			AudioQualityVoteDao.State.resolveConfig(env);
			fail("missing required DB_* key must be a configuration error");
		} catch (IllegalStateException expected) {
			// no implicit defaults
		}
	}

	@Test
	public void completeKeysAssembleUrlAndStripTrailingNewline() throws Exception {
		try (TempSecretFile secret = new TempSecretFile("file-secret\n")) {
			Map<String, String> env = env("DB_HOST", "mariadb", "DB_PORT", "3306", "DB_NAME", "audiovote",
					"DB_USER", "audiovote", "DB_PASSWORD_FILE", secret.path);
			AudioQualityVoteDao.State.resolveConfig(env);
			assertEquals("jdbc:mariadb://mariadb:3306/audiovote?allowMultiQueries=true",
					AudioQualityVoteDao.State.jdbcUrl);
			assertEquals("audiovote", AudioQualityVoteDao.State.user);
			assertEquals("file-secret", AudioQualityVoteDao.State.password);
		}
	}

	@Test
	public void directPasswordFormResolves() {
		Map<String, String> env = env("DB_HOST", "db.example", "DB_PORT", "3307", "DB_NAME", "av",
				"DB_USER", "avuser", "DB_PASSWORD", "direct-pw");
		AudioQualityVoteDao.State.resolveConfig(env);
		assertEquals("jdbc:mariadb://db.example:3307/av?allowMultiQueries=true", AudioQualityVoteDao.State.jdbcUrl);
		assertEquals("direct-pw", AudioQualityVoteDao.State.password);
	}

	/** A secret file deleted on close. */
	private static final class TempSecretFile implements AutoCloseable {
		final String path;
		private final java.io.File file;

		TempSecretFile(String content) throws java.io.IOException {
			file = java.io.File.createTempFile("dbsecret", ".txt");
			try (java.io.FileWriter w = new java.io.FileWriter(file)) {
				w.write(content);
			}
			path = file.getAbsolutePath();
		}

		@Override
		public void close() {
			file.delete();
		}
	}
}
