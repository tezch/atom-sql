package io.github.tezch.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * org.springframework.jdbc.core.BatchPreparedStatementSetter
 * @author tezch
 */
public interface BatchPreparedStatementSetter {

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#setValues(PreparedStatement, int)
	 * @param ps
	 * @param i
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, int i) throws SQLException;

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#getBatchSize()
	 * @return int
	 */
	int getBatchSize();
}
