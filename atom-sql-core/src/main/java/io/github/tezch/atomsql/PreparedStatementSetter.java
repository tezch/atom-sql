package io.github.tezch.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import io.github.tezch.atomsql.internal.AtomSqlUtils;

/**
 * org.springframework.jdbc.core.BatchPreparedStatementSetter
 * @author tezch
 */
@FunctionalInterface
public interface PreparedStatementSetter {

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#setValues(PreparedStatement)
	 * @param ps
	 * @throws SQLException
	 */
	default void setValues(PreparedStatement ps) throws SQLException {
		setValues(ps, AtomSqlUtils.stackTrace());
	}

	/**
	 * @see #setValues(PreparedStatement)
	 * @param ps
	 * @param stackTrace
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, Optional<StackTraceElement[]> stackTrace) throws SQLException;
}
