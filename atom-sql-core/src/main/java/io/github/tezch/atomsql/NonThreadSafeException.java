package io.github.tezch.atomsql;

import io.github.tezch.atomsql.annotation.NonThreadSafe;

/**
 * {@link AtomSqlType}に定義されている型のなかで{@link NonThreadSafe}が付与されている型を使用した場合に投げられる例外です。
 * @author tezch
 */
public class NonThreadSafeException extends RuntimeException {

	private static final long serialVersionUID = -9143790236887713121L;

	/**
	 * 単一のコンストラクタです。
	 */
	public NonThreadSafeException() {
		super(NonThreadSafe.class.getName() + " values can only be used within " + AtomSql.class.getName() + "#tryNonThreadSafe");
	}
}
