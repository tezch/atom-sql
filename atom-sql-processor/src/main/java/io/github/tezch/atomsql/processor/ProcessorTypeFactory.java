package io.github.tezch.atomsql.processor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

import io.github.tezch.atomsql.AtomSql;
import io.github.tezch.atomsql.AtomSqlType;
import io.github.tezch.atomsql.AtomSqlTypeFactory;
import io.github.tezch.atomsql.AtomSqlUtils;

/**
 * @author tezch
 */
class ProcessorTypeFactory {

	static final ProcessorTypeFactory instance = new ProcessorTypeFactory();

	final AtomSqlTypeFactory atomSqlTypeFactory = AtomSqlTypeFactory.newInstance(
		AtomSql.configure().typeFactoryClass(),
		AtomSqlTypeFactory.class.getClassLoader());

	AtomSqlType typeOf(String name) {
		return atomSqlTypeFactory.typeOf(Objects.requireNonNull(name)).orElseGet(() -> {
			if (Arrays.stream(name.split("\\.")).filter(w -> !AtomSqlUtils.isSafeJavaIdentifier(w)).findFirst().isPresent()) {
				//Javaシンボルに使用できない文字が含まれていた場合
				throw new IllegalStateException(name);
			}

			//processor内で、参照できないクラスの名称から自動生成クラスのフィールドを生成するためのタイプ
			return new ENUM_EXPRESSION_TYPE(name);
		});
	}

	AtomSqlType typeArgumentOf(String name) {
		return typeOf(name).toTypeArgument();
	}

	static class ENUM_EXPRESSION_TYPE implements AtomSqlType {

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
			return enumClass;
		}

		@Override
		public String typeArgumentExpression() {
			return typeExpression();
		}
	}
}
