package io.github.tezch.atomsql.spring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.AtomSqlUtils;
import io.github.tezch.atomsql.Configure;
import io.github.tezch.atomsql.Endpoint;
import io.github.tezch.atomsql.Endpoints;
import io.github.tezch.atomsql.SimpleConfigure;
import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * Atom SQLをSpringで使用できるように初期化するクラスです。<br>
 * {@link SqlProxy}が付与されたクラスを{@link Autowired}可能にします。<br>
 * application.propertiesにプレフィックスatomsqlを付加することで各設定を記述することが可能です<br>
 * @author tezch
 */
public class AtomSqlContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	private final Function<JdbcTemplate, Endpoint> endpointBuilder;

	/**
	 * デフォルトコンストラクタです。
	 */
	public AtomSqlContextInitializer() {
		endpointBuilder = jdbcTemplate -> new JdbcTemplateEndpoint(jdbcTemplate);
	}

	/**
	 * {@link JdbcTemplate}を利用する{@link Endpoint}を独自に生成するする際に使用するコンストラクタです。
	 * @param endpointBuilder
	 */
	public AtomSqlContextInitializer(Function<JdbcTemplate, Endpoint> endpointBuilder) {
		this.endpointBuilder = Objects.requireNonNull(endpointBuilder);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
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

		var shouldIgnoreNoSqlLog = environment.getProperty("atomsql.should-ignore-no-sql-log", Boolean.class, false);

		var usesQualifier = environment.getProperty("atomsql.uses-qualifier", Boolean.class, false);

		var typeFactoryClass = environment.getProperty("atomsql.type-factory-class", "");

		var batchThreshold = environment.getProperty("atomsql.batch-threshold", Integer.class, 0);

		var usesAtomCache = environment.getProperty("atomsql.uses-atom-cache", Boolean.class, true);

		return new SimpleConfigure(
			enableLog,
			Pattern.compile(logStackTracePattern),
			shouldIgnoreNoSqlLog,
			usesQualifier,
			typeFactoryClass,
			batchThreshold,
			usesAtomCache);
	}

	private Endpoints endpoints(GenericApplicationContext context) {
		var map = context.getBeansOfType(JdbcTemplate.class);
		var primary = context.getBean(JdbcTemplate.class);

		if (map.size() == 1) {
			return new Endpoints(endpointBuilder.apply(primary));
		}

		var entries = map.entrySet().stream().map(e -> {
			var jdbcTemplate = e.getValue();
			return new Endpoints.Entry(e.getKey(), endpointBuilder.apply(jdbcTemplate), jdbcTemplate == primary);
		}).toArray(Endpoints.Entry[]::new);

		return new Endpoints(entries);
	}
}
