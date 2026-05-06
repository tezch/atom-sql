package io.github.tezch.atomsql.spring;

import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.Configuration;
import io.github.tezch.atomsql.SimpleConfiguration;
import io.github.tezch.atomsql.SqlService;
import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * Atom SQLをSpringで使用できるように初期化するクラスです。<br>
 * {@link SqlProxy}が付与されたクラスを{@link Autowired}可能にします。<br>
 * application.propertiesにプレフィックスatomsqlを付加することで各設定を記述することが可能です<br>
 * @author tezch
 */
public class AtomSqlContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	/**
	 * Springのプロパティに記載するAtom SQL関連プロパティのプレフィクス
	 */
	public static final String PROPERTIES_PREFIX = "atomsql";

	private final Function<JdbcTemplate, SqlService> sqlServiceBuilder;

	/**
	 * デフォルトコンストラクタです。
	 */
	public AtomSqlContextInitializer() {
		sqlServiceBuilder = jdbcTemplate -> new JdbcTemplateSqlService(jdbcTemplate);
	}

	/**
	 * {@link JdbcTemplate}を利用する{@link SqlService}を独自に生成するする際に使用するコンストラクタです。
	 * @param sqlServiceBuilder
	 */
	public AtomSqlContextInitializer(@SuppressWarnings("exports") Function<JdbcTemplate, SqlService> sqlServiceBuilder) {
		this.sqlServiceBuilder = Objects.requireNonNull(sqlServiceBuilder);
	}

	@Override
	public void initialize(@SuppressWarnings("exports") GenericApplicationContext context) {
		var customizer = AtomSqlInitializer.beanDefinitionCustomizer();

		AtomSql.initializeIfUninitialized(configuration(context));
		context.registerBean(AtomSql.class, () -> new AtomSql(AtomSqlInitializer.sqlServices(context, sqlServiceBuilder)), customizer);

		AtomSqlInitializer.registerAllSqlProxies(c -> {
			context.registerBean(c, () -> context.getBean(AtomSql.class).of(c), customizer);
		});
	}

	private static Configuration configuration(GenericApplicationContext context) {
		var environment = context.getEnvironment();
		var enableLog = environment.getProperty(PROPERTIES_PREFIX + ".enable-log", Boolean.class, false);

		var logStackTracePattern = environment.getProperty(PROPERTIES_PREFIX + ".log-stacktrace-pattern", ".+");

		var shouldIgnoreNoSqlLog = environment.getProperty(PROPERTIES_PREFIX + ".should-ignore-no-sql-log", Boolean.class, false);

		var usesQualifier = environment.getProperty(PROPERTIES_PREFIX + ".uses-qualifier", Boolean.class, false);

		var typeFactoryClass = environment.getProperty(PROPERTIES_PREFIX + ".type-factory-class", "");

		var batchThreshold = environment.getProperty(PROPERTIES_PREFIX + ".batch-threshold", Integer.class, 0);

		var cacheCapacity = environment.getProperty(
			PROPERTIES_PREFIX + ".cache-capacity",
			Integer.class,
			Integer.parseInt(AtomSql.DEFAULT_CACHE_SIZE));

		return new SimpleConfiguration(
			enableLog,
			logStackTracePattern,
			shouldIgnoreNoSqlLog,
			usesQualifier,
			typeFactoryClass,
			batchThreshold,
			cacheCapacity);
	}
}
