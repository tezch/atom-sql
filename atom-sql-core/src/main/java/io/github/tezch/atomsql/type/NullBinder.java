package io.github.tezch.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.github.tezch.atomsql.AtomSqlException;

class NullBinder {

	static int bind(int index, PreparedStatement statement, int sqlType) {
		try {
			statement.setNull(index, sqlType);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}
}
