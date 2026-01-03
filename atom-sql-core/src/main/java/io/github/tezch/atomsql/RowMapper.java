package io.github.tezch.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * org.springframework.jdbc.core.RowMapper
 * @author tezch
 * @param <T>
 */
@FunctionalInterface
public interface RowMapper<T> {

	/**
	 * org.springframework.jdbc.core.RowMapper#mapRow(ResultSet, int)
	 * @param rs
	 * @param rowNum
	 * @return T
	 * @throws SQLException
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
