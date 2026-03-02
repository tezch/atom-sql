package io.github.tezch.atomsql;

import io.github.tezch.atomsql.annotation.NoSqlLog;
import io.github.tezch.atomsql.annotation.Qualifier;

/**
 * Atom SQL用の設定を保持するレコードです。
 * @author tezch
 * @param enableLog SQLログを出力するかどうか
 * @param logStacktracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
 * @param shouldIgnoreNoSqlLog アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか
 * @param usesQualifier {@link Qualifier}を使用するかどうか
 * @param typeFactoryClass {@link AtomSqlTypeFactory}
 * @param batchThreshold バッチ更新時の閾値
 * @param cacheCapacity キャッシュの最大値
 */
public record SimpleConfigure(
	boolean enableLog,
	String logStacktracePattern,
	boolean shouldIgnoreNoSqlLog,
	boolean usesQualifier,
	String typeFactoryClass,
	int batchThreshold,
	int cacheCapacity) implements Configure {

	/**
	 * スタブ設定
	 * @return スタブ設定
	 */
	public static Configure stub() {
		return new SimpleConfigure(false, null, false, false, null, 0, 0);
	}
}
