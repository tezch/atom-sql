package io.github.tezch.atomsql.spring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.AtomSqlUtils;
import io.github.tezch.atomsql.Configure;
import io.github.tezch.atomsql.Endpoints;
import io.github.tezch.atomsql.SimpleConfigure;
import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * Atom SQLをSpringで使用できるように初期化するクラスです。<br>
 * {@link SqlProxy}が付与されたクラスを{@link Autowired}可能にします。<br>
 * application.propertiesにプレフィックスatomsqlを付加することで各設定を記述することが可能です<br>
 * @see SpringApplication#addInitializers(ApplicationContextInitializer...)
 * @author tezch
 */
public class AtomSqlContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(@SuppressWarnings("exports") GenericApplicationContext context) {
		AtomSql.initializeIfUninitialized(configure(context));

		List<Class<?>> classes;
		try {
			classes = AtomSqlUtils.loadProxyClasses();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		BeanDefinitionCustomizer customizer = bd -> {
			bd.setScope(BeanDefinition.SCOPE_SINGLETON);
			bd.setLazyInit(true);
			bd.setAutowireCandidate(true);
			bd.setPrimary(true);
		};

		context.registerBean(AtomSql.class, () -> new AtomSql(endpoints(context)), customizer);

		classes.forEach(c -> {
			@SuppressWarnings("unchecked")
			var casted = (Class<Object>) c;
			context.registerBean(casted, () -> context.getBean(AtomSql.class).of(casted), customizer);
		});
	}

	private static Configure configure(GenericApplicationContext context) {
		var environment = context.getEnvironment();
		var enableLog = environment.getProperty("atomsql.enable-log", Boolean.class, false);

		var logStackTracePattern = environment.getProperty("atomsql.log-stacktrace-pattern", ".+");

		var ignoreNoSqlLog = environment.getProperty("atomsql.ignore-no-sql-log", Boolean.class, false);

		var useQualifier = environment.getProperty("atomsql.use-qualifier", Boolean.class, false);

		var typeFactoryClass = environment.getProperty("atomsql.type-factory-class", "");

		var batchThreshold = environment.getProperty("atomsql.batch-threshold", Integer.class, 0);

		return new SimpleConfigure(
			enableLog,
			Pattern.compile(logStackTracePattern),
			ignoreNoSqlLog,
			useQualifier,
			typeFactoryClass,
			batchThreshold);
	}

	private static Endpoints endpoints(GenericApplicationContext context) {
		var map = context.getBeansOfType(JdbcTemplate.class);
		var primary = context.getBean(JdbcTemplate.class);

		if (map.size() == 1) {
			return new Endpoints(new JdbcTemplateEndpoint(primary));
		}

		var entries = map.entrySet().stream().map(e -> {
			var jdbcTemplate = e.getValue();
			return new Endpoints.Entry(e.getKey(), new JdbcTemplateEndpoint(jdbcTemplate), jdbcTemplate == primary);
		}).toArray(Endpoints.Entry[]::new);

		return new Endpoints(entries);
	}
}
