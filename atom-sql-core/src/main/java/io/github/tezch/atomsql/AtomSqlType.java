package io.github.tezch.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.github.tezch.atomsql.annotation.NonThreadSafe;

/**
 * Atom SQLで使用可能な型を表すインターフェイスです。
 * @author tezch
 */
public interface AtomSqlType {

	/**
	 * この型に対応するJavaでの型を返します。
	 * @return type
	 */
	abstract Class<?> type();

	/**
	 * この型に合ったメソッドで{@link PreparedStatement}に値をセットします。
	 * @param index
	 * @param statement
	 * @param value
	 * @return 次index
	 * @throws SQLException
	 */
	int bind(int index, PreparedStatement statement, Object value) throws SQLException;

	/**
	 * この型に合ったメソッドで{@link ResultSet}から値を取得します。
	 * @param rs
	 * @param columnLabel
	 * @return 値
	 * @throws SQLException
	 */
	Object get(ResultSet rs, String columnLabel) throws SQLException;

	/**
	 * この型に合ったメソッドで{@link ResultSet}から値を取得します。
	 * @param rs
	 * @param columnIndex
	 * @return 値
	 * @throws SQLException
	 */
	Object get(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * この型が型パラメータで使用される場合の代替型を返します。<br>
	 * 主にプリミティブな型がラッパークラスの型に変換するために使用します。
	 * @return {@link AtomSqlType}
	 */
	AtomSqlType toTypeArgument();

	/**
	 * この型の型ヒント(SQL内での表現)を返します。
	 * @return 型ヒント文字列
	 */
	String typeHint();

	/**
	 * この型がスレッドセーフではないかを返します。
	 * @return nonThreadSafeの場合、true
	 */
	default boolean nonThreadSafe() {
		return getClass().isAnnotationPresent(NonThreadSafe.class);
	}

	/**
	 * SQL内で使用するプレースホルダ文字列表現を返します。
	 * @param value
	 * @return プレースホルダ文字列表現
	 */
	default String placeholderExpression(Object value) {
		return "?";
	}

	/**
	 * この型の文字列表現を返します。
	 * @return 型文字列表現
	 */
	default String typeExpression() {
		return type().getName();
	}

	/**
	 * この型が型パラメーターとして使用される際の文字列表現を返します。<br>
	 * 独自型を使用する時、サブクラスすべてを表現したい場合に<code>? extends SomeClass</code>のように使用できます。
	 * @return 型パラメーター文字列表現
	 */
	default String typeArgumentExpression() {
		return type().getName();
	}

	/**
	 * この型が型パラメータを必要とするかを返します。
	 * @return この型が型パラメータを必要とするか
	 */
	default boolean needsTypeArgument() {
		return false;
	}
}
