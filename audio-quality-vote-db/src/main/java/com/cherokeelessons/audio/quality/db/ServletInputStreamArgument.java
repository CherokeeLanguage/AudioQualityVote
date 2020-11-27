package com.cherokeelessons.audio.quality.db;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

public class ServletInputStreamArgument implements Argument {

	private InputStream is;
	public ServletInputStreamArgument(InputStream stream) {
		is=stream;
	}
	
	@Override
	public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        if (is == null) {
            statement.setNull(position, Types.LONGVARBINARY);
        } else {
            statement.setBinaryStream(position, is);
        }
	}

}
