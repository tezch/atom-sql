package io.github.tezch.atomsql.spring.boot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.spring.AtomSqlInitializer;
import io.github.tezch.atomsql.spring.JdbcTemplateEndpoint;

/**
 * AtomSqlAutoConfiguration
 * @author tezch
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
@Import(SqlProxyRegistrar.class)
@EnableConfigurationProperties(AtomSqlProperties.class)
public class AtomSqlAutoConfiguration {

	/**
	 * {@link AtomSql}
	 * @param config
	 * @param context
	 * @return {@link AtomSql}
	 */
	@Bean
	@ConditionalOnMissingBean
	AtomSql atomSql(AtomSqlProperties config, GenericApplicationContext context) {
		AtomSql.initializeIfUninitialized(config);

		JdbcTemplateEndpointFactory factory;

		var className = config.jdbcTemplateEndpointFactoryClass();
		if (className == null || className.isBlank()) {
			factory = t -> new JdbcTemplateEndpoint(t);
		} else {
			try {
				factory = (JdbcTemplateEndpointFactory) Class.forName(
					className,
					true,
					Thread.currentThread().getContextClassLoader()).getConstructor().newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		return new AtomSql(AtomSqlInitializer.endpoints(context, factory));
	}
}
