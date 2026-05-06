package io.github.tezch.atomsql.spring.boot;

import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.SqlService;

/**
 * {@link JdbcTemplate}を使用した{@link SqlService}の作成方法を定義したインターフェイスです。
 */
@FunctionalInterface
public interface JdbcTemplateSqlServiceFactory extends Function<JdbcTemplate, SqlService> {

	@Override
	SqlService apply(JdbcTemplate jdbcTemplate);
}
