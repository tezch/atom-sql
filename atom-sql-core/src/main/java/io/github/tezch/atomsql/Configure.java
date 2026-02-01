package io.github.tezch.atomsql;

import java.util.regex.Pattern;

import io.github.tezch.atomsql.annotation.NoSqlLog;
import io.github.tezch.atomsql.annotation.Qualifier;
import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author tezch
 */
public interface Configure {

	/**
	 * enable-log<br>
	 * SQLログを出力するかどうか<br>
	 * SQLのログ出力を行う場合、true
	 * @return SQLログを出力するかどうか
	 */
	boolean enableLog();

	/**
	 * log-stacktrace-pattern<br>
	 * SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）<br>
	 * パターンにマッチしたものがログに出力される
	 * @return フィルタパターン
	 */
	Pattern logStackTracePattern();

	/**
	 * should-ignore-no-sql-log<br>
	 * アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか<br>
	 * 無視してSQLのログ出力を行う場合、true
	 * @return {@link NoSqlLog}を無視するか
	 */
	boolean shouldIgnoreNoSqlLog();

	/**
	 * uses-qualifier<br>
	 * {@link Qualifier}を使用するかどうか
	 * @return 使用する場合、true
	 */
	boolean usesQualifier();

	/**
	 * type-factory-class<br>
	 * @return {@link AtomSqlTypeFactory}のFQCN
	 */
	String typeFactoryClass();

	/**
	 * batch-threshold<br>
	 * バッチ更新時の閾値<br>
	 * この値を超えるとバッチ実行され、件数がリセットされる<br>
	 * この値が0以下の場合、閾値はないものとして扱われる
	 * @return バッチ更新時の閾値
	 */
	int batchThreshold();

	/**
	 * uses-atom-cache<br>
	 * {@link SqlProxy}のメソッド呼び出しで生成される{@link Atom}をキャッシュするかどうか
	 * @return 使用する場合、true
	 */
	boolean usesAtomCache();
}
