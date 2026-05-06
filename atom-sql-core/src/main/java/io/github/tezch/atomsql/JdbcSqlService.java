package io.github.tezch.atomsql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.tezch.atomsql.internal.SimpleConnectionProxy;

/**
 * JDBCを使用した{@link SqlService}の簡易実装クラスです。
 * @author tezch
 */
public class JdbcSqlService implements SqlService {

	private final Supplier<Connection> supplier;

	private final ThreadLocal<Connection> connection = new ThreadLocal<>();

	/**
	 * 単一のコンストラクタです。
	 * @param supplier {@link Connection}の供給元
	 */
	public JdbcSqlService(Supplier<Connection> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
	}

	@Override
	public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
		try (var conn = connection()) {
			try (var ps = conn.prepareStatement(AtomSql.NEW_LINE + sql)) {
				var size = bpss.getBatchSize();
				for (var i = 0; i < size; i++) {
					bpss.setValues(ps, i);
					ps.addBatch();
				}

				return ps.executeBatch();
			}
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public <T> Stream<T> queryForStream(
		String sql,
		PreparedStatementSetter pss,
		RowMapper<T> rowMapper,
		SqlProxySnapshot snapshot) {
		Connection conn = connection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(AtomSql.NEW_LINE + sql);

			pss.setValues(ps);

			rs = ps.executeQuery();

			var stream = StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
					new ResultSetIterator<T>(rs, rowMapper),

					Spliterator.NONNULL | Spliterator.IMMUTABLE),
				false);

			var fps = ps;
			var frs = rs;
			stream.onClose(() -> {
				try {
					close(frs, fps, conn);
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				}
			});

			return stream;
		} catch (SQLException e) {
			try {
				close(rs, ps, conn);
			} catch (SQLException ex) {
				e.addSuppressed(ex);
			}

			throw new AtomSqlException(e);
		} catch (Exception e) {
			try {
				close(rs, ps, conn);
			} catch (SQLException ex) {
				e.addSuppressed(ex);
			}

			throw new IllegalStateException(e);
		}
	}

	private static void close(ResultSet rs, PreparedStatement ps, Connection conn) throws SQLException {
		try (rs; ps; conn) {}
	}

	@Override
	public int update(String sql, PreparedStatementSetter pss, SqlProxySnapshot snapshot) {
		try (var conn = connection()) {
			try (var ps = conn.prepareStatement(AtomSql.NEW_LINE + sql)) {
				pss.setValues(ps);

				return ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public void logSql(
		Logger logger,
		String originalSql,
		String sql,
		PreparedStatement ps,
		SqlProxySnapshot snapshot) {
		logger.log(Level.INFO, "sql:" + AtomSql.NEW_LINE + ps.toString());
	}

	@Override
	public void logSensitiveSql(
		Logger logger,
		String originalSql,
		String sql,
		List<BindingValue> bindingValues,
		SqlProxySnapshot snapshot) {
		logger.log(Level.INFO, "sensitive sql:" + AtomSql.NEW_LINE + originalSql);
		logger.log(Level.INFO, "binding values:");

		bindingValues.forEach(p -> logger.log(Level.INFO, p.name() + ": " + p.value()));
	}

	@Override
	public void borrowConnection(Consumer<ConnectionProxy> consumer) {
		try (var conn = supplier.get()) {
			connection.set(conn);

			consumer.accept(new SimpleConnectionProxy(conn));
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		} finally {
			connection.remove();
		}
	}

	/**
	 * borrowConnection中は同一のConnectionの使用を強制する
	 */
	private Connection connection() {
		var con = connection.get();
		return con == null ? supplier.get() : con;
	}

	private static class ResultSetIterator<T> implements Iterator<T> {

		private final ResultSet rs;

		private final RowMapper<T> rowMapper;

		private boolean hasNext;

		private int rowNum;

		private ResultSetIterator(ResultSet rs, RowMapper<T> rowMapper) throws SQLException {
			this.rs = rs;
			this.rowMapper = rowMapper;
			hasNext = rs.next();
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			if (!hasNext) throw new IllegalStateException();

			try {
				var row = rowMapper.mapRow(rs, ++rowNum);

				hasNext = rs.next();

				return row;
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}
	}
}
