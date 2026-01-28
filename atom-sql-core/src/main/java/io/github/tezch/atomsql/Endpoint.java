package io.github.tezch.atomsql;

import java.lang.System.Logger;
import java.sql.PreparedStatement;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Atom SQLで使用する機能のみを操作可能にするためのインターフェイスです。
 * @author tezch
 */
public interface Endpoint {

	/**
	 * バッチ更新を実行します。
	 * @param sql
	 * @param bpss
	 * @return affected rows
	 */
	int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss);

	/**
	 * SELECT等検索結果のあるsqlを実行します。
	 * @param <T>
	 * @param sql
	 * @param pss
	 * @param rowMapper
	 * @param snapshot
	 * @return {@link Stream}
	 */
	<T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper, SqlProxySnapshot snapshot);

	/**
	 * UPDATE, INSERT等のsqlを実行します。
	 * @param sql
	 * @param pss
	 * @param snapshot
	 * @return int
	 */
	int update(String sql, PreparedStatementSetter pss, SqlProxySnapshot snapshot);

	/**
	 * SQLログ出力を行う設定にしている場合、実装に合わせたSQL文をログ出力します。
	 * @see Configure#enableLog
	 * @param logger {@link Logger}
	 * @param originalSql プレースホルダ変換前のSQL
	 * @param sql プレースホルダ変換後のSQL
	 * @param ps プレースホルダ変換後SQLセット済みの{@link PreparedStatement}
	 * @param snapshot {@link SqlProxySnapshot}
	 */
	void logSql(Logger logger, String originalSql, String sql, PreparedStatement ps, SqlProxySnapshot snapshot);

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。
	 * @param consumer
	 */
	void bollowConnection(Consumer<ConnectionProxy> consumer);
}
