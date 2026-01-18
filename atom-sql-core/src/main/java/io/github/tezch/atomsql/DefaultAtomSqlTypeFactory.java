package io.github.tezch.atomsql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.tezch.atomsql.annotation.StringEnum;
import io.github.tezch.atomsql.type.BIG_DECIMAL;
import io.github.tezch.atomsql.type.BINARY_STREAM;
import io.github.tezch.atomsql.type.BLOB;
import io.github.tezch.atomsql.type.BOOLEAN;
import io.github.tezch.atomsql.type.BYTE_ARRAY;
import io.github.tezch.atomsql.type.CHARACTER_STREAM;
import io.github.tezch.atomsql.type.CLOB;
import io.github.tezch.atomsql.type.CSV;
import io.github.tezch.atomsql.type.DATE;
import io.github.tezch.atomsql.type.DATETIME;
import io.github.tezch.atomsql.type.DOUBLE;
import io.github.tezch.atomsql.type.ENUM;
import io.github.tezch.atomsql.type.FLOAT;
import io.github.tezch.atomsql.type.INTEGER;
import io.github.tezch.atomsql.type.LONG;
import io.github.tezch.atomsql.type.OBJECT;
import io.github.tezch.atomsql.type.P_BOOLEAN;
import io.github.tezch.atomsql.type.P_DOUBLE;
import io.github.tezch.atomsql.type.P_FLOAT;
import io.github.tezch.atomsql.type.P_INT;
import io.github.tezch.atomsql.type.P_LONG;
import io.github.tezch.atomsql.type.STRING;
import io.github.tezch.atomsql.type.STRING_ENUM;
import io.github.tezch.atomsql.type.TIME;

/**
 * {@link AtomSqlTypeFactory}のデフォルト実装です。
 * @author tezch
 */
public class DefaultAtomSqlTypeFactory implements AtomSqlTypeFactory {

	private final Map<Class<?>, AtomSqlType> typeMap = new HashMap<>();

	private final Map<String, AtomSqlType> nameMap = new HashMap<>();

	private static final AtomSqlType[] singletonTypes = {
		BIG_DECIMAL.instance,
		BINARY_STREAM.instance,
		BLOB.instance,
		BOOLEAN.instance,
		BYTE_ARRAY.instance,
		CHARACTER_STREAM.instance,
		CLOB.instance,
		DATE.instance,
		DATETIME.instance,
		DOUBLE.instance,
		FLOAT.instance,
		INTEGER.instance,
		LONG.instance,
		OBJECT.instance,
		P_BOOLEAN.instance,
		P_DOUBLE.instance,
		P_FLOAT.instance,
		P_INT.instance,
		P_LONG.instance,
		STRING.instance,
		TIME.instance,
	};

	private static final AtomSqlType[] nonPrimitiveTypes = {
		BIG_DECIMAL.instance,
		BINARY_STREAM.instance,
		BLOB.instance,
		BOOLEAN.instance,
		BYTE_ARRAY.instance,
		CHARACTER_STREAM.instance,
		CLOB.instance,
		DATE.instance,
		DATETIME.instance,
		DOUBLE.instance,
		FLOAT.instance,
		INTEGER.instance,
		LONG.instance,
		OBJECT.instance,
		STRING.instance,
		TIME.instance,
	};

	/**
	 * singleton
	 */
	public static AtomSqlTypeFactory instance = new DefaultAtomSqlTypeFactory();

	/**
	 * constructor
	 */
	protected DefaultAtomSqlTypeFactory() {
		Arrays.stream(singletonTypes).forEach(b -> {
			typeMap.put(b.type(), b);
			nameMap.put(b.getClass().getSimpleName(), b);
		});

		CSV csv = new CSV(this);
		typeMap.put(csv.type(), csv);
		nameMap.put(CSV.class.getSimpleName(), csv);
	}

	@Override
	public AtomSqlType select(Class<?> c) {
		var type = typeMap.get(Objects.requireNonNull(c));

		if (type == null) {
			if (!c.isEnum()) throw new UnknownSqlTypeException(c);

			@SuppressWarnings("unchecked")
			var enumClass = (Class<? extends Enum<?>>) c;

			if (enumClass.getAnnotation(StringEnum.class) != null) return new STRING_ENUM(enumClass);

			return new ENUM(enumClass);
		}

		return type;
	}

	@Override
	public Optional<AtomSqlType> typeOf(String name) {
		return Optional.ofNullable(nameMap.get(Objects.requireNonNull(name)));
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
