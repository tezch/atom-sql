package io.github.tezch.atomsql.spring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tezch.atomsql.AtomSqlUtils;
import io.github.tezch.atomsql.Endpoint;
import io.github.tezch.atomsql.Endpoints;
import io.github.tezch.atomsql.annotation.SqlProxy;

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
	 * @param endpointBuilder {@link JdbcTemplate}から{@link Endpoint}を生成
	 * @return {@link Endpoints}
	 */
	public static Endpoints endpoints(
		@SuppressWarnings("exports") GenericApplicationContext context,
		@SuppressWarnings("exports") Function<JdbcTemplate, Endpoint> endpointBuilder) {
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
