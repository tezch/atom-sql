package io.github.tezch.atomsql.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link SqlProxy}のメソッドパラメータに付与可能な、機密情報を含む値であることを表すアノテーションです。<br>
 * SQLにバインドされる値がパスワード等ログに出力したくない機密項目を含む場合、このアノテーションをそのメソッドパラメータに付与することでログにバインド値が出力されなくなります。<br>
 * @author tezch
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Confidential {}
