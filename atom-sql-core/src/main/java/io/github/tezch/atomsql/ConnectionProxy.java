package io.github.tezch.atomsql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;

/**
 * {@link Connection}を機能制限したインターフェイスです。<br>
 * {@link Connection}から取得できる各種オブジェクトを、{@link Connection}を直接操作することなく取得させます。
 * @author tezch
 */
public interface ConnectionProxy {

	/**
	 * @see Connection#createBlob()
	 * @return {@link Blob}
	 */
	Blob createBlob();

	/**
	 * @see Connection#createClob()
	 * @return {@link Clob}
	 */
	Clob createClob();

	/**
	 * @see Connection#commit()
	 */
	void commit();

	/**
	 * @see Connection#rollback()
	 */
	void rollback();
}
