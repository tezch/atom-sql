package io.github.tezch.atomsql.spring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.SqlService;
import io.github.tezch.atomsql.SqlServices;
import io.github.tezch.atomsql.annotation.SqlProxy;
import io.github.tezch.atomsql.internal.AtomSqlUtils;

/**
 * Spring FrameworkとSpring Bootでの設定処理の共通ロジック。
 * @author tezch
 */
public class AtomSqlInitializer {

	/**
	 * beanDefinitionCustomizer
	 * @return {@link BeanDefinitionCustomizer}
	 */
	@SuppressWarnings("exports")
	public static BeanDefinitionCustomizer beanDefinitionCustomizer() {
		return bd -> {
			bd.setScope(BeanDefinition.SCOPE_SINGLETON);
			bd.setLazyInit(true);
			bd.setAutowireCandidate(true);
			bd.setPrimary(true);
		};
	}

	/**
	 * 全ての{@link SqlProxy}を使用可能にします。
	 * @param SqlProxyRegistrar {@link SqlProxy}実装クラス登録処理
	 */
	public static void registerAllSqlProxies(Consumer<Class<Object>> SqlProxyRegistrar) {
		try {
			AtomSqlUtils.loadProxyClasses().forEach(c -> {
				@SuppressWarnings("unchecked")
				var casted = (Class<Object>) c;
				SqlProxyRegistrar.accept(casted);
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 
	 * @param context {@link GenericApplicationContext}
	 * @param sqlServiceBuilder {@link JdbcTemplate}から{@link SqlService}を生成
	 * @return {@link SqlServices}
	 */
	public static SqlServices sqlServices(
		@SuppressWarnings("exports") GenericApplicationContext context,
		@SuppressWarnings("exports") Function<JdbcTemplate, SqlService> sqlServiceBuilder) {
		var map = context.getBeansOfType(JdbcTemplate.class);
		var primary = context.getBean(JdbcTemplate.class);

		if (map.size() == 1) {
			return new SqlServices(sqlServiceBuilder.apply(primary));
		}

		var entries = map.entrySet().stream().map(e -> {
			var jdbcTemplate = e.getValue();
			return new SqlServices.Entry(e.getKey(), sqlServiceBuilder.apply(jdbcTemplate), jdbcTemplate == primary);
		}).toArray(SqlServices.Entry[]::new);

		return new SqlServices(entries);
	}
}
