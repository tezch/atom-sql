package io.github.tezch.atomsql.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.AtomSqlTypeFactory;
import io.github.tezch.atomsql.Configuration;
import io.github.tezch.atomsql.annotation.NoSqlLog;
import io.github.tezch.atomsql.annotation.Qualifier;
import io.github.tezch.atomsql.spring.AtomSqlContextInitializer;

/**
 * AtomSqlProperties
 * @author tezch
 * @param enableLog SQLログを出力するかどうか
 * @param logStacktracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
 * @param shouldIgnoreNoSqlLog アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか
 * @param usesQualifier {@link Qualifier}を使用するかどうか
 * @param typeFactoryClass {@link AtomSqlTypeFactory}のFQCN
 * @param jdbcTemplateSqlServiceFactoryClass {@link JdbcTemplateSqlServiceFactory}のFQCN
 * @param batchThreshold バッチ更新時の閾値
 * @param cacheCapacity キャッシュの最大値
 */
@ConfigurationProperties(prefix = AtomSqlContextInitializer.PROPERTIES_PREFIX)
public record AtomSqlProperties(
	@DefaultValue("false") boolean enableLog,
	@DefaultValue(".+") String logStacktracePattern,
	@DefaultValue("false") boolean shouldIgnoreNoSqlLog,
	@DefaultValue("false") boolean usesQualifier,
	@DefaultValue("") String typeFactoryClass,
	@DefaultValue("") String jdbcTemplateSqlServiceFactoryClass,
	@DefaultValue("0") int batchThreshold,
	@DefaultValue(AtomSql.DEFAULT_CACHE_SIZE) int cacheCapacity) implements Configuration {}
