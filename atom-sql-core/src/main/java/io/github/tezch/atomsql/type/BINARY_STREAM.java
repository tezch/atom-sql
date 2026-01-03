package io.github.tezch.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import io.github.tezch.atomsql.AtomSqlException;
import io.github.tezch.atomsql.AtomSqlType;
import io.github.tezch.atomsql.BinaryStream;
import io.github.tezch.atomsql.annotation.NonThreadSafe;

/**
 * {@link BinaryStream}
 */
@NonThreadSafe
public class BINARY_STREAM implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BINARY_STREAM();

	private BINARY_STREAM() {}

	@Override
	public Class<?> type() {
		return BinaryStream.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.LONGVARBINARY);

		var stream = (BinaryStream) value;
		try {
			statement.setBinaryStream(index, stream.input, stream.length);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var stream = rs.getBinaryStream(columnLabel);
		return rs.wasNull() ? null : new BinaryStream(stream, -1);
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		var stream = rs.getBinaryStream(columnIndex);
		return rs.wasNull() ? null : new BinaryStream(stream, -1);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}
