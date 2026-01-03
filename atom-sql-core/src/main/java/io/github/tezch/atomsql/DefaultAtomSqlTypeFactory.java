package io.github.tezch.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	private DefaultAtomSqlTypeFactory() {
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
	public AtomSqlType typeOf(String name) {
		var type = nameMap.get(Objects.requireNonNull(name));

		if (type != null) return type;

		if (Arrays.stream(name.split("\\.")).filter(w -> !AtomSqlUtils.isSafeJavaIdentifier(w)).findFirst().isPresent()) {
			//Javaシンボルに使用できない文字が含まれていた場合
			throw new UnknownSqlTypeNameException(name);
		}

		//processor内で、参照できないクラスの名称から自動生成クラスのフィールドを生成するためのタイプ
		return new ENUM_EXPRESSION_TYPE(name);
	}

	@Override
	public AtomSqlType typeArgumentOf(String name) {
		return typeOf(name).toTypeArgument();
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

	private static class ENUM_EXPRESSION_TYPE implements AtomSqlType {

		private final String enumClass;

		private ENUM_EXPRESSION_TYPE(String enumClass) {
			this.enumClass = enumClass;
		}

		@Override
		public Class<?> type() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) {
			throw new UnsupportedOperationException();
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
			return this;
		}

		@Override
		public String typeExpression() {
			//enumClassとしてなんでも記述できないように、Enumの型パラメータとして表現することで
			//Enumではないクラスを指定された場合コンパイルエラーを発生させる
			return Enum.class.getName() + "<" + enumClass + ">";
		}

		@Override
		public String typeArgumentExpression() {
			return typeExpression();
		}
	}
}
