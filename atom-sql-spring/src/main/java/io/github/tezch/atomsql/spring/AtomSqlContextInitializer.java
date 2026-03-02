package io.github.tezch.atomsql.spring;

import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.Configure;
import io.github.tezch.atomsql.Constants;
import io.github.tezch.atomsql.Endpoint;
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
	public AtomSqlContextInitializer(@SuppressWarnings("exports") Function<JdbcTemplate, Endpoint> endpointBuilder) {
		this.endpointBuilder = Objects.requireNonNull(endpointBuilder);
	}

	@Override
	public void initialize(@SuppressWarnings("exports") GenericApplicationContext context) {
		var customizer = AtomSqlInitializer.beanDefinitionCustomizer();

		AtomSql.initializeIfUninitialized(configure(context));
		context.registerBean(AtomSql.class, () -> new AtomSql(AtomSqlInitializer.endpoints(context, endpointBuilder)), customizer);

		AtomSqlInitializer.registerAllSqlProxies(c -> {
			context.registerBean(c, () -> context.getBean(AtomSql.class).of(c), customizer);
		});
	}

	private static Configure configure(GenericApplicationContext context) {
		var environment = context.getEnvironment();
		var enableLog = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".enable-log", Boolean.class, false);

		var logStackTracePattern = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".log-stacktrace-pattern", ".+");

		var shouldIgnoreNoSqlLog = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".should-ignore-no-sql-log", Boolean.class, false);

		var usesQualifier = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".uses-qualifier", Boolean.class, false);

		var typeFactoryClass = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".type-factory-class", "");

		var batchThreshold = environment.getProperty(SpringConstants.PROPERTIES_PREFIX + ".batch-threshold", Integer.class, 0);

		var cacheCapacity = environment.getProperty(
			SpringConstants.PROPERTIES_PREFIX + ".cache-capacity",
			Integer.class,
			Integer.parseInt(Constants.DEFAULT_CACHE_SIZE));

		return new SimpleConfigure(
			enableLog,
			logStackTracePattern,
			shouldIgnoreNoSqlLog,
			usesQualifier,
			typeFactoryClass,
			batchThreshold,
			cacheCapacity);
	}
}
