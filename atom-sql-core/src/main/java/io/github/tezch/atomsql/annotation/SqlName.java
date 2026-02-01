package io.github.tezch.atomsql.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import io.github.tezch.atomsql.Atom;

/**
 * {@link Atom}を生成するための印として使用するアノテーションです。<br>
 * {@link Atom}の値はフィールド名そのものとなります。<br>
 * SQL文内で使用したいテーブル名やカラム名などのオブジェクトを、直接Javaのフィールドとして定義することが可能です。
 * @see io.github.tezch.atomsql.Atom#of(Field)
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SqlName {

	/**
	 * フィールド名の変換形式を指定します。
	 * @return {@link Casing}
	 */
	Casing casing() default Casing.ORIGINAL;

	/**
	 * フィールド名の変換形式
	 */
	enum Casing {

		/**
		 * @see String#toLowerCase()
		 */
		LOWER,

		/**
		 * @see String#toUpperCase()
		 */
		UPPER,

		ORIGINAL;
	}
}
