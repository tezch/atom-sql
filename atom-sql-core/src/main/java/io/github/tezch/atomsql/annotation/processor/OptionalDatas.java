package io.github.tezch.atomsql.annotation.processor;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author tezch
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface OptionalDatas {

	/**
	 * @return {@link OptionalData}
	 */
	OptionalData[] value();
}
