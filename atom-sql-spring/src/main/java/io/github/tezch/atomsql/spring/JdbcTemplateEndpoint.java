package io.github.tezch.atomsql.spring;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.BatchPreparedStatementSetter;
import io.github.tezch.atomsql.ConnectionProxy;
import io.github.tezch.atomsql.Constants;
import io.github.tezch.atomsql.Endpoint;
import io.github.tezch.atomsql.PreparedStatementSetter;
import io.github.tezch.atomsql.RowMapper;
import io.github.tezch.atomsql.SimpleConnectionProxy;
import io.github.tezch.atomsql.SqlProxySnapshot;

/**
 * @author tezch
 */
public class JdbcTemplateEndpoint implements Endpoint {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * @param jdbcTemplate
	 */
	public JdbcTemplateEndpoint(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
	}

	/**
	 * @see JdbcTemplate#batchUpdate(String, org.springframework.jdbc.core.BatchPreparedStatementSetter)
	 */
	@Override
	public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		return jdbcTemplate.batchUpdate(Constants.NEW_LINE + sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				bpss.setValues(ps, i);
			}

			@Override
			public int getBatchSize() {
				return bpss.getBatchSize();
			}
		});

	}

	/**
	 * @see JdbcTemplate#queryForStream(String, org.springframework.jdbc.core.PreparedStatementSetter, org.springframework.jdbc.core.RowMapper)
	 */
	@Override
	public <T> Stream<T> queryForStream(
		String sql,
		PreparedStatementSetter pss,
		RowMapper<T> rowMapper,
		SqlProxySnapshot snapshot) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		return jdbcTemplate.queryForStream(Constants.NEW_LINE + sql, (ps) -> pss.setValues(ps), (rs, rowNum) -> rowMapper.mapRow(rs, rowNum));
	}

	/**
	 * @see JdbcTemplate#update(String, org.springframework.jdbc.core.PreparedStatementSetter)
	 */
	@Override
	public int update(String sql, PreparedStatementSetter pss, SqlProxySnapshot snapshot) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		return jdbcTemplate.update(Constants.NEW_LINE + sql, (ps) -> pss.setValues(ps));
	}

	@Override
	public void logSql(
		Logger logger,
		String originalSql,
		String sql,
		PreparedStatement ps,
		SqlProxySnapshot snapshot) {
		logger.log(Level.INFO, "sql:" + Constants.NEW_LINE + ps.toString());
	}

	@Override
	public void logConfidentialSql(
		Logger logger,
		String originalSql,
		String sql,
		List<BindingValue> bindingValues,
		SqlProxySnapshot snapshot) {
		logger.log(Level.INFO, "confidential sql:" + Constants.NEW_LINE + originalSql);
		logger.log(Level.INFO, "binding values:");

		bindingValues.forEach(p -> logger.log(Level.INFO, p.name() + ": " + p.value()));
	}

	@Override
	public void bollowConnection(Consumer<ConnectionProxy> consumer) {
		jdbcTemplate.execute((ConnectionCallback<Object>) con -> {
			consumer.accept(new SimpleConnectionProxy(con));

			return null;
		});
	}
}
