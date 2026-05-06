package io.github.tezch.atomsql;

import static io.github.tezch.atomsql.DefaultAtomSqlType.BIG_DECIMAL;
import static io.github.tezch.atomsql.DefaultAtomSqlType.BINARY_STREAM;
import static io.github.tezch.atomsql.DefaultAtomSqlType.BLOB;
import static io.github.tezch.atomsql.DefaultAtomSqlType.BOOLEAN;
import static io.github.tezch.atomsql.DefaultAtomSqlType.BYTE_ARRAY;
import static io.github.tezch.atomsql.DefaultAtomSqlType.CHARACTER_STREAM;
import static io.github.tezch.atomsql.DefaultAtomSqlType.CLOB;
import static io.github.tezch.atomsql.DefaultAtomSqlType.DATE;
import static io.github.tezch.atomsql.DefaultAtomSqlType.DATETIME;
import static io.github.tezch.atomsql.DefaultAtomSqlType.DOUBLE;
import static io.github.tezch.atomsql.DefaultAtomSqlType.FLOAT;
import static io.github.tezch.atomsql.DefaultAtomSqlType.INTEGER;
import static io.github.tezch.atomsql.DefaultAtomSqlType.LONG;
import static io.github.tezch.atomsql.DefaultAtomSqlType.OBJECT;
import static io.github.tezch.atomsql.DefaultAtomSqlType.P_BOOLEAN;
import static io.github.tezch.atomsql.DefaultAtomSqlType.P_DOUBLE;
import static io.github.tezch.atomsql.DefaultAtomSqlType.P_FLOAT;
import static io.github.tezch.atomsql.DefaultAtomSqlType.P_INT;
import static io.github.tezch.atomsql.DefaultAtomSqlType.P_LONG;
import static io.github.tezch.atomsql.DefaultAtomSqlType.STRING;
import static io.github.tezch.atomsql.DefaultAtomSqlType.TIME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.tezch.atomsql.annotation.StringEnum;
import io.github.tezch.atomsql.type.CsvType;
import io.github.tezch.atomsql.type.EnumType;
import io.github.tezch.atomsql.type.StringEnumType;

/**
 * {@link AtomSqlTypeFactory}のデフォルト実装です。
 * @author tezch
 */
public class DefaultAtomSqlTypeFactory implements AtomSqlTypeFactory {

	private final Map<Class<?>, AtomSqlType> typeMap = new HashMap<>();

	private final Map<String, AtomSqlType> hintMap = new HashMap<>();

	private static final AtomSqlType[] defaultTypes = {
		BIG_DECIMAL,
		BINARY_STREAM,
		BLOB,
		BOOLEAN,
		BYTE_ARRAY,
		CHARACTER_STREAM,
		CLOB,
		DATE,
		DATETIME,
		DOUBLE,
		FLOAT,
		INTEGER,
		LONG,
		OBJECT,
		P_BOOLEAN,
		P_DOUBLE,
		P_FLOAT,
		P_INT,
		P_LONG,
		STRING,
		TIME,
	};

	private static final AtomSqlType[] nonPrimitiveTypes = {
		BIG_DECIMAL,
		BINARY_STREAM,
		BLOB,
		BOOLEAN,
		BYTE_ARRAY,
		CHARACTER_STREAM,
		CLOB,
		DATE,
		DATETIME,
		DOUBLE,
		FLOAT,
		INTEGER,
		LONG,
		OBJECT,
		STRING,
		TIME,
	};

	/**
	 * singleton
	 */
	public static AtomSqlTypeFactory instance = new DefaultAtomSqlTypeFactory();

	/**
	 * constructor
	 */
	protected DefaultAtomSqlTypeFactory() {
		Arrays.stream(defaultTypes).forEach(b -> {
			typeMap.put(b.type(), b);
			hintMap.put(b.typeHint(), b);
		});

		CsvType csv = new CsvType(this);
		typeMap.put(csv.type(), csv);
		hintMap.put(csv.typeHint(), csv);
	}

	@Override
	public AtomSqlType select(Class<?> c) {
		var type = typeMap.get(Objects.requireNonNull(c));

		if (type == null) {
			if (!c.isEnum()) throw new UnknownSqlTypeException(c);

			@SuppressWarnings("unchecked")
			var enumClass = (Class<? extends Enum<?>>) c;

			if (enumClass.isAnnotationPresent(StringEnum.class)) return new StringEnumType(enumClass);

			return new EnumType(enumClass);
		}

		return type;
	}

	@Override
	public Optional<AtomSqlType> typeOf(String name) {
		return Optional.ofNullable(hintMap.get(Objects.requireNonNull(name)));
	}

	@Override
	public boolean canUse(Class<?> c) {
		if (c.isEnum()) return true;

		return Arrays.stream(nonPrimitiveTypes).map(t -> t.type()).filter(t -> t.equals(c)).findFirst().isPresent();
	}

	@Override
	public AtomSqlType[] nonPrimitiveTypes() {
		return nonPrimitiveTypes.clone();
	}
}
