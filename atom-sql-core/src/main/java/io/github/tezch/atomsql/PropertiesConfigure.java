package io.github.tezch.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

import io.github.tezch.atomsql.annotation.NoSqlLog;
import io.github.tezch.atomsql.annotation.Qualifier;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author tezch
 */
public class PropertiesConfigure implements Configure {

	private final String configFileName = "atom-sql.properties";

	/**
	 * enable-log<br>
	 * SQLログを出力するかどうか<br>
	 * SQLのログ出力を行う場合、true
	 */
	private final boolean enableLog;

	/**
	 * log-stacktrace-pattern<br>
	 * SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）<br>
	 * パターンにマッチしたものがログに出力される
	 */
	private final String logStackTracePattern;

	/**
	 * should-ignore-no-sql-log<br>
	 * アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか<br>
	 * 無視してSQLのログ出力を行う場合、true
	 */
	private final boolean shouldIgnoreNoSqlLog;

	/**
	 * uses-qualifier<br>
	 * {@link Qualifier}を使用するかどうか
	 */
	private final boolean usesQualifier;

	/**
	 * type-factory-class<br>
	 * {@link AtomSqlTypeFactory}のFQCN
	 */
	private final String typeFactoryClass;

	/**
	 * batch-threshold<br>
	 * バッチ更新時の閾値<br>
	 * この値を超えるとバッチ実行され、件数がリセットされる<br>
	 * この値が0以下の場合、閾値はないものとして扱われる
	 */
	private final int batchThreshold;

	/**
	 * cache-capacity<br>
	 * キャッシュの最大値<br>
	 * この値を超えると古いキャッシュから削除される<br>
	 * この値が0以下の場合、キャッシュは行わない
	 */
	private final int cacheCapacity;

	/**
	 * クラスパスのルートにあるatom-sql.propertiesから設定を読み込みインスタンスを作成します。
	 */
	public PropertiesConfigure() {
		var config = new Properties();

		try (var input = PropertiesConfigure.class.getClassLoader().getResourceAsStream(configFileName)) {
			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "false"));

		logStackTracePattern = config.getProperty("log-stacktrace-pattern", ".+");

		shouldIgnoreNoSqlLog = Boolean.valueOf(config.getProperty("should-ignore-no-sql-log", "false"));

		usesQualifier = Boolean.valueOf(config.getProperty("uses-qualifier", "false"));

		typeFactoryClass = config.getProperty("type-factory-class", null);

		batchThreshold = Integer.parseInt(config.getProperty("batch-threshold", "0"));

		cacheCapacity = Integer.parseInt(
			Optional.ofNullable(config.getProperty("cache-capacity")).orElse(Constants.DEFAULT_CACHE_SIZE));
	}

	@Override
	public boolean enableLog() {
		return enableLog;
	}

	@Override
	public String logStacktracePattern() {
		return logStackTracePattern;
	}

	@Override
	public boolean shouldIgnoreNoSqlLog() {
		return shouldIgnoreNoSqlLog;
	}

	@Override
	public boolean usesQualifier() {
		return usesQualifier;
	}

	@Override
	public String typeFactoryClass() {
		return typeFactoryClass;
	}

	@Override
	public int batchThreshold() {
		return batchThreshold;
	}

	@Override
	public int cacheCapacity() {
		return cacheCapacity;
	}
}
