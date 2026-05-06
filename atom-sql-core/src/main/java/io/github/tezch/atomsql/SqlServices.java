package io.github.tezch.atomsql;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.tezch.atomsql.annotation.Qualifier;

/**
 * {@link SqlService}を複数管理するためのマップクラスです。
 * @author tezch
 */
public class SqlServices {

	private final Entry primary;

	private final Map<String, Entry> map;

	/**
	 * 複数の{@link SqlService}を設定してインスタンスを生成します。
	 * @param entries
	 */
	public SqlServices(Entry... entries) {
		if (entries.length == 0) throw new IllegalArgumentException("Empty entries");

		map = new LinkedHashMap<>();
		Entry primary = null;
		for (var entry : entries) {
			if (entry.primary()) {
				if (primary != null) throw new IllegalArgumentException("Primary entry is duplicate");
				primary = entry;
			}

			map.put(entry.name(), entry);
		}

		if (primary == null) throw new IllegalArgumentException("Primary entry not found");

		this.primary = primary;
	}

	/**
	 * 単体の{@link SqlService}を設定してインスタンスを生成します。
	 * @param primarySqlService
	 */
	public SqlServices(SqlService primarySqlService) {
		this(new Entry(null, primarySqlService, true));
	}

	/**
	 * {@link Qualifier}の値をもとに対応する{@link Entry}を返します。
	 * @param name Bean名 nullの場合プライマリ{@link SqlService}が返却される
	 * @return {@link Entry}
	 */
	public Entry get(String name) {
		return map.get(name);
	}

	/**
	 * プライマリ{@link SqlService}を返します。
	 * @return {@link Entry}
	 */
	public Entry get() {
		return primary;
	}

	/**
	 * {@link SqlServices}用要素
	 * @param name {@link Qualifier}名
	 * @param sqlService {@link SqlService}
	 * @param primary プライマリBeanかどうか
	 */
	public static record Entry(String name, SqlService sqlService, boolean primary) {}
}
