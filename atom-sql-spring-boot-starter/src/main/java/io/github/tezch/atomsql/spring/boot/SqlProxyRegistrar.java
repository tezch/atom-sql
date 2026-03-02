package io.github.tezch.atomsql.spring.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.spring.AtomSqlInitializer;

/**
 * SqlProxyRegistrar
 * @author tezch
 */
public class SqlProxyRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		AtomSqlInitializer.registerAllSqlProxies(c -> {
			var builder = BeanDefinitionBuilder.genericBeanDefinition(c, () -> {
				var atomSql = beanFactory.getBean(AtomSql.class);
				return atomSql.of(c);
			});

			builder.applyCustomizers(AtomSqlInitializer.beanDefinitionCustomizer());

			registry.registerBeanDefinition(c.getName(), builder.getBeanDefinition());
		});
	}
}
