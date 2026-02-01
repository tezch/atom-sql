package io.github.tezch.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author tezch
 */
public interface BatchPreparedStatementSetter {

	/**
	 * @param ps
	 * @param i
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, int i) throws SQLException;

	/**
	 * @param i
	 * @return {@link SqlProxySnapshot}
	 */
	SqlProxySnapshot sqlProxySnapshot(int i);

	/**
	 * @return int
	 */
	int getBatchSize();
}
