package io.github.tezch.atomsql;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import io.github.tezch.atomsql.annotation.NonThreadSafe;
import io.github.tezch.atomsql.internal.AtomSqlUtils;

public enum DefaultAtomSqlType implements AtomSqlType {

	/**
	 * @see BigDecimal
	 */
	BIG_DECIMAL {

		@Override
		public Class<?> type() {
			return BigDecimal.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setBigDecimal(index, (BigDecimal) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBigDecimal(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getBigDecimal(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see BinaryStream
	 */
	@NonThreadSafe
	BINARY_STREAM {

		@Override
		public Class<?> type() {
			return BinaryStream.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.LONGVARBINARY);

			var stream = (BinaryStream) value;

			statement.setBinaryStream(index, stream.input, stream.length);

			return index + 1;
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
	},

	/**
	 * @see Blob
	 */
	@NonThreadSafe
	BLOB {

		@Override
		public Class<?> type() {
			return Blob.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setBlob(index, (Blob) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBlob(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getBlob(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Boolean
	 */
	BOOLEAN {

		@Override
		public Class<?> type() {
			return Boolean.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.BOOLEAN);

			statement.setBoolean(index, (boolean) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getBoolean(columnLabel);

			return rs.wasNull() ? null : value;
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getBoolean(columnIndex);

			return rs.wasNull() ? null : value;
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * byte[]
	 */
	@NonThreadSafe
	BYTE_ARRAY {

		@Override
		public Class<?> type() {
			return byte[].class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setBytes(index, (byte[]) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBytes(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getBytes(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}

		@Override
		public String typeExpression() {
			return "byte[]";
		}

		@Override
		public String typeArgumentExpression() {
			//Csvの型パラメータとして使用できない
			throw new UnsupportedOperationException();
		}
	},

	/**
	 * @see CharacterStream
	 */
	@NonThreadSafe
	CHARACTER_STREAM {

		@Override
		public Class<?> type() {
			return CharacterStream.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.LONGNVARCHAR);

			var stream = (CharacterStream) value;

			statement.setCharacterStream(index, stream.input, stream.length);
			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var stream = rs.getCharacterStream(columnLabel);

			return rs.wasNull() ? null : new CharacterStream(stream, -1);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var stream = rs.getCharacterStream(columnIndex);

			return rs.wasNull() ? null : new CharacterStream(stream, -1);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Clob
	 */
	@NonThreadSafe
	CLOB {

		@Override
		public Class<?> type() {
			return Clob.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setClob(index, (Clob) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getClob(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getClob(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see LocalDate
	 */
	DATE {

		@Override
		public Class<?> type() {
			return LocalDate.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.DATE);

			statement.setDate(index, Date.valueOf((LocalDate) value));

			return index + 1;
		}

		@Override
		public LocalDate get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getDate(columnLabel);

			return value == null ? null : value.toLocalDate();
		}

		@Override
		public LocalDate get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getDate(columnIndex);

			return value == null ? null : value.toLocalDate();
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see LocalDateTime
	 */
	DATETIME {

		@Override
		public Class<?> type() {
			return LocalDateTime.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.TIMESTAMP);

			statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));

			return index + 1;
		}

		@Override
		public LocalDateTime get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getTimestamp(columnLabel);

			return value == null ? null : value.toLocalDateTime();
		}

		@Override
		public LocalDateTime get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getTimestamp(columnIndex);

			return value == null ? null : value.toLocalDateTime();
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Double
	 */
	DOUBLE {

		@Override
		public Class<?> type() {
			return Double.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.DOUBLE);

			statement.setDouble(index, (double) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getDouble(columnLabel);

			return rs.wasNull() ? null : value;
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getDouble(columnIndex);

			return rs.wasNull() ? null : value;
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Float
	 */
	FLOAT {

		@Override
		public Class<?> type() {
			return Float.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.FLOAT);

			statement.setFloat(index, (float) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getFloat(columnLabel);

			return rs.wasNull() ? null : value;
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getFloat(columnIndex);

			return rs.wasNull() ? null : value;
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Integer
	 */
	INTEGER {

		@Override
		public Class<?> type() {
			return Integer.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.INTEGER);

			statement.setInt(index, (int) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getInt(columnLabel);

			return rs.wasNull() ? null : value;
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getInt(columnIndex);

			return rs.wasNull() ? null : value;
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see Long
	 */
	LONG {

		@Override
		public Class<?> type() {
			return Long.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.BIGINT);

			statement.setLong(index, (long) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getLong(columnLabel);

			return rs.wasNull() ? null : value;
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getLong(columnIndex);

			return rs.wasNull() ? null : value;
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * null
	 */
	NULL {

		@Override
		public Class<?> type() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			//データベース製品によってはエラーとなる可能性がある
			//その場合は型ヒントを使用すること
			statement.setNull(index, Types.NULL);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AtomSqlType toTypeArgument() {
			throw new UnsupportedOperationException();
		}
	},

	/**
	 * @see Object
	 */
	@NonThreadSafe
	OBJECT {

		@Override
		public Class<?> type() {
			return Object.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setObject(index, value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getObject(columnLabel);

		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getObject(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * boolean
	 */
	P_BOOLEAN {

		@Override
		public Class<?> type() {
			return boolean.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setBoolean(index, (boolean) value);

			return index + 1;
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
			return BOOLEAN;
		}
	},

	/**
	 * double
	 */
	P_DOUBLE {

		@Override
		public Class<?> type() {
			return double.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setDouble(index, (double) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getDouble(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getDouble(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return DOUBLE;
		}
	},

	/**
	 * float
	 */
	P_FLOAT {

		@Override
		public Class<?> type() {
			return float.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setFloat(index, (float) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getFloat(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getFloat(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return FLOAT;
		}
	},

	/**
	 * int
	 */
	P_INT {

		@Override
		public Class<?> type() {
			return int.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setInt(index, (int) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getInt(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getInt(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return INTEGER;
		}
	},

	/**
	 * long
	 */
	P_LONG {

		@Override
		public Class<?> type() {
			return long.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setLong(index, (long) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getLong(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getLong(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return LONG;
		}
	},

	/**
	 * @see String
	 */
	STRING {

		@Override
		public Class<?> type() {
			return String.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			statement.setString(index, (String) value);

			return index + 1;
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getString(columnLabel);
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			return rs.getString(columnIndex);
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * @see LocalTime
	 */
	TIME {

		@Override
		public Class<?> type() {
			return LocalTime.class;
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) throws SQLException {
			if (value == null) return AtomSqlUtils.bindAsNull(index, statement, Types.TIME);

			statement.setTime(index, Time.valueOf((LocalTime) value));

			return index + 1;
		}

		@Override
		public LocalTime get(ResultSet rs, String columnLabel) throws SQLException {
			var value = rs.getTime(columnLabel);

			return value == null ? null : value.toLocalTime();
		}

		@Override
		public LocalTime get(ResultSet rs, int columnIndex) throws SQLException {
			var value = rs.getTime(columnIndex);

			return value == null ? null : value.toLocalTime();
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}
	};

	@Override
	public String typeHint() {
		return name();
	}

	@Override
	public boolean nonThreadSafe() {
		try {
			return getClass().getField(name()).isAnnotationPresent(NonThreadSafe.class);
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}
}
