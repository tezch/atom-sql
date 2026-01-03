package io.github.tezch.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.github.tezch.atomsql.AtomSqlException;
import io.github.tezch.atomsql.AtomSqlType;

/**
 * boolean
 */
public class P_BOOLEAN implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_BOOLEAN();

	private P_BOOLEAN() {}

	@Override
	public Class<?> type() {
		return boolean.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setBoolean(index, (boolean) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getBoolean(columnLabel);
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getBoolean(columnIndex);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return io.github.tezch.atomsql.type.BOOLEAN.instance;
	}
}
