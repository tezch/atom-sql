package io.github.tezch.atomsql.spring.boot;

import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.Endpoint;

/**
 * {@link JdbcTemplate}を使用した{@link Endpoint}の作成方法を定義したインターフェイスです。
 */
@FunctionalInterface
public interface JdbcTemplateEndpointFactory extends Function<JdbcTemplate, Endpoint> {

	@Override
	Endpoint apply(JdbcTemplate jdbcTemplate);
}
