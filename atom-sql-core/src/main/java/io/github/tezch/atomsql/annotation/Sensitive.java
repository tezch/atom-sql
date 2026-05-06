package io.github.tezch.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * SQLにバインドされる値が、パスワード等ログに出力したくない機密情報であることを表すアノテーションです。<br>
 * {@SqlProxy}のメソッドに付与した場合、そのメソッドで使用するSQLにバインドする全ての値、またはvalueを指定した場合はそれらの値<br>
 * {@SqlProxy}のメソッドパラメータに付与した場合、そのパラメータの値のみ<br>
 * {@SqlProxy}のメソッドパラメータが自動生成されるパラメータバインダの場合はSQLにバインドする全ての値、またはvalueを指定した場合はそれらの値<br>
 * となります。
 * @author tezch
 */
@Target({ METHOD, PARAMETER })
@Retention(RUNTIME)
public @interface Sensitive {

	/**
	 * 値をログに出力したくないパラメータ名の配列を返します。
	 * @return パラメーター名の配列
	 */
	String[] value() default {};
}
