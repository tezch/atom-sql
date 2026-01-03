package io.github.tezch.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.tezch.atomsql.Endpoint;

/**
 * {@link Endpoint}を登録する対象となる識別子を表します。
 * @author tezch
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Qualifier {

	/**
	 * @return 識別子
	 */
	String value();
}
