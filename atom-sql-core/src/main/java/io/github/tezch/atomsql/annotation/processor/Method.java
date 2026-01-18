package io.github.tezch.atomsql.annotation.processor;

import java.util.function.Consumer;

import io.github.tezch.atomsql.Protoatom;
import io.github.tezch.atomsql.annotation.DataObject;
import io.github.tezch.atomsql.annotation.OptionalColumn;

/**
 * @author tezch
 */
public @interface Method {

	/**
	 * メソッド名
	 * @return メソッド名
	 */
	String name();

	/**
	 * メソッドの引数名
	 * @return メソッドの引数名
	 */
	String[] parameters();

	/**
	 * メソッドの引数の型
	 * @return メソッドの引数の型
	 */
	Class<?>[] parameterTypes();

	/**
	 * メソッドの引数が{@link OptionalColumn}アノテーションを持つか
	 * @return メソッドの引数が{@link OptionalColumn}アノテーションを持つか
	 */
	boolean[] parameterOptionalColumns() default {};

	/**
	 * メソッドの{@link Consumer}に指定されたクラス
	 * @return メソッドの{@link Consumer}に指定されたクラス
	 */
	Class<?> parameterBinder() default Object.class;

	/**
	 * 戻り値の型パラメータで示されるAtom SQL検索結果で使用可能なクラス化または{@link DataObject}クラス
	 * @return 戻り値の型パラメータで示されるAtom SQL検索結果で使用可能なクラス、または{@link DataObject}クラス
	 */
	Class<?> result() default Object.class;

	/**
	 * {@link Protoatom}に指定されたクラス
	 * @return {@link Protoatom} に指定されたクラス
	 */
	Class<?> protoatomImplanter() default Object.class;
}
