package io.github.tezch.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.github.tezch.atomsql.AtomSqlType;
import io.github.tezch.atomsql.AtomSqlTypeFactory;
import io.github.tezch.atomsql.Csv;
import io.github.tezch.atomsql.annotation.DataObject;

/**
 * Comma Separated Values<br>
 * {@link DataObject}では使用できません。
 * @see Csv
 */
public class CSV implements AtomSqlType {

	private final AtomSqlTypeFactory typeFactory;

	/**
	 * コンストラクタ
	 * @param typeFactory 値の型判定用
	 */
	public CSV(AtomSqlTypeFactory typeFactory) {
		this.typeFactory = typeFactory;
	}

	@Override
	public Class<?> type() {
		return Csv.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		var values = ((Csv<?>) value).values();

		var size = values.size();
		IntStream.range(0, size).forEach(i -> {
			var v = values.get(i);

			AtomSqlType type;
			if (v == null) {
				type = NULL.instance;
			} else {
				type = typeFactory.select(v.getClass());
			}

			type.bind(index + i, statement, v);
		});

		return index + size;
	}

	@Override
	public String placeholderExpression(Object value) {
		var values = ((Csv<?>) value).values();
		return String.join(", ", values.stream().map(v -> "?").toList());
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		throw new UnsupportedOperationException();
	}

	private static final ThreadLocal<NonThreadSafeSource> nonThreadSafeSource = new ThreadLocal<>();

	private final record NonThreadSafeSource(Object value, AtomSqlTypeFactory factory) {};

	/**
	 * supplier内の処理で型の{@link AtomSqlType#nonThreadSafe()}を使用する際、CSVが含まれていた時のために、判定に必要な値をあらかじめセットしておきます。
	 * @param value {@link Csv}
	 * @param factory {@link AtomSqlTypeFactory}
	 * @param supplier 判定処理
	 * @return 判定処理の返却値
	 */
	public static boolean tryNonThreadSafe(Object value, AtomSqlTypeFactory factory, Supplier<Boolean> supplier) {
		try {
			nonThreadSafeSource.set(new NonThreadSafeSource(value, factory));

			return supplier.get();
		} finally {
			nonThreadSafeSource.remove();
		}
	}

	@Override
	public boolean nonThreadSafe() {
		var source = nonThreadSafeSource.get();

		//値なしで判定する場合(processorでのstatic判定)、型パラメータで判定を行うのでここではfalseを返す
		if (source == null) return false;

		if (!(source.value instanceof Csv csv)) throw new IllegalStateException("Illegal state");

		return ((Csv<?>) csv).values().stream().filter(v -> isNonThreadSafe(v, typeFactory)).findFirst().isPresent();
	}

	private static boolean isNonThreadSafe(Object value, AtomSqlTypeFactory factory) {
		if (value == null) return false;

		var type = factory.select(value.getClass());

		//Csvの値としてCsvを使用することはできません
		if (type instanceof CSV) throw new IllegalStateException("Cannot use Csv as Csv value");

		return type.nonThreadSafe();
	}

	@Override
	public boolean needsTypeArgument() {
		return true;
	}
}
