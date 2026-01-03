package io.github.tezch.atomsql.annotation.processor;

import java.util.Optional;

/**
 * {@link Optional}検索結果項目
 * @author tezch
 */
public @interface OptionalData {

	/**
	 * フィールド名またはレコードコンポーネント名
	 * @return フィールド名またはレコードコンポーネント名
	 */
	String name();

	/**
	 * 型引数
	 * @return 型引数
	 */
	Class<?> type();
}
