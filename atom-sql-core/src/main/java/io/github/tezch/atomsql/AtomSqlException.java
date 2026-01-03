package io.github.tezch.atomsql;

import java.sql.SQLException;

/**
 * {@link SQLException}を検査なし例外にするためのラッパー例外クラスです。
 * @author tezch
 */
public class AtomSqlException extends RuntimeException {

	private static final long serialVersionUID = -1595978181006028117L;

	/**
	 * @param cause
	 */
	public AtomSqlException(SQLException cause) {
		super(cause);
	}
}
