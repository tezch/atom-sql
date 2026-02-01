package io.github.tezch.atomsql;

import java.lang.System.Logger.Level;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import io.github.tezch.atomsql.Endpoint.BindingValue;
import io.github.tezch.atomsql.InnerSql.Element;
import io.github.tezch.atomsql.InnerSql.Placeholder;
import io.github.tezch.atomsql.InnerSql.Text;
import io.github.tezch.atomsql.annotation.OptionalColumn;
import io.github.tezch.atomsql.annotation.processor.Methods;
import io.github.tezch.atomsql.annotation.processor.OptionalDatas;
import io.github.tezch.atomsql.annotation.processor.TooManyColumnsDataObject;

class SqlProxyHelper implements PreparedStatementSetter {

	final InnerSql sql;

	final Endpoints.Entry entry;

	private final Class<?> resultClass;

	private final AtomSqlTypeFactory typeFactory;

	private final SqlLogger sqlLogger;

	private final SqlProxySnapshot snapshot;

	SqlProxyHelper(
		SecureString secureSql,
		Endpoints.Entry entry,
		String[] confidentials,
		String[] parameterNames,
		AtomSqlType[] parameterTypes,
		Class<?> resultClass,
		Object[] args,
		AtomSqlTypeFactory typeFactory,
		SqlLogger sqlLogger,
		SqlProxySnapshot snapshot) {
		this.entry = entry;
		this.resultClass = resultClass;
		this.typeFactory = typeFactory;
		this.sqlLogger = sqlLogger;
		this.snapshot = snapshot;

		Map<String, TypeAndArg> map = new HashMap<>();
		for (int i = 0; i < parameterNames.length; i++) {
			map.put(
				parameterNames[i],
				new TypeAndArg(parameterTypes[i], args[i]));
		}

		List<Element> elements = new LinkedList<>();

		var confidentialSet = confidentials(confidentials, parameterNames);

		var sql = ColumnFinder.normalize(secureSql.toString());

		var sqlRemain = PlaceholderFinder.execute(sql, f -> {
			elements.add(new Text(new SecureString(f.gap)));

			if (!map.containsKey(f.placeholder))
				throw new PlaceholderNotFoundException(f.placeholder);

			var typeAndArg = map.get(f.placeholder);
			var value = typeAndArg.arg();
			var type = typeAndArg.type();

			elements.add(
				new Placeholder(
					new SecureString(f.placeholder),
					confidentialSet.contains(f.placeholder),
					new SecureString(type.placeholderExpression(value)),
					f.all,
					type,
					value,
					typeFactory));
		});

		elements.add(new Text(new SecureString(sqlRemain)));

		this.sql = new InnerSql(elements);
	}

	SqlProxyHelper(SqlProxyHelper base, Class<?> newDataObjectClass) {
		this.sql = base.sql;
		this.entry = base.entry;
		this.resultClass = newDataObjectClass;
		this.typeFactory = base.typeFactory;
		this.sqlLogger = base.sqlLogger;
		this.snapshot = base.snapshot;
	}

	SqlProxyHelper(
		InnerSql sql,
		SqlProxyHelper main) {
		this.sql = sql;
		this.entry = main.entry;
		this.resultClass = main.resultClass;
		this.typeFactory = main.typeFactory;
		this.sqlLogger = main.sqlLogger;
		this.snapshot = main.snapshot;
	}

	SqlProxySnapshot sqlProxySnapshot() {
		return snapshot;
	}

	Object createDataObject(ResultSet rs) {
		if (resultClass == Object.class)
			throw new IllegalStateException();

		if (resultClass.isRecord()) {
			return createRecordDataObject(rs);
		}

		//検索結果が単一の値の場合
		if (typeFactory.canUse(resultClass)) {
			try {
				return typeFactory.select(resultClass).get(rs, 1);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		Constructor<?> constructor;
		try {
			constructor = resultClass.getConstructor(ResultSet.class);
		} catch (NoSuchMethodException e) {
			return createDefaultConstructorClassDataObject(rs);
		}

		try {
			return constructor.newInstance(rs);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Set<String> confidentials(String[] confidentials, String[] parameterNames) {
		if (confidentials == null) return Collections.emptySet();

		//ConfidentialSqlが付与されているが、valueが指定されていない場合、すべて機密扱い
		if (confidentials.length == 0) {
			return new HashSet<>(Arrays.asList(parameterNames));
		}

		return new HashSet<>(Arrays.asList(confidentials));
	}

	private static Set<String> columnNamesFrom(ResultSet rs) {
		try {
			var metaData = rs.getMetaData();

			var count = metaData.getColumnCount();

			return new HashSet<>(IntStream.range(1, count + 1).mapToObj(i -> {
				try {
					return metaData.getColumnName(i).toUpperCase();
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				}
			}).toList());
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	private Object createRecordDataObject(ResultSet rs) {
		Methods methods;
		try {
			methods = Class.forName(
				resultClass.getName() + Constants.METADATA_CLASS_SUFFIX,
				true,
				Thread.currentThread().getContextClassLoader()).getAnnotation(Methods.class);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}

		var method = methods.value()[0];
		var parameterNames = method.parameters();
		var parameterTypes = method.parameterTypes();
		var parameters = new Object[parameterNames.length];
		var optionalColumns = method.parameterOptionalColumns();

		var resultSetColumns = columnNamesFrom(rs);

		var optionals = new Optionals();
		for (var i = 0; i < parameterNames.length; i++) {
			var parameterType = parameterTypes[i];
			var parameterName = parameterNames[i];
			var isOptionalColumn = optionalColumns[i];

			var needsOptional = false;
			if (Optional.class.equals(parameterType)) {
				parameterType = optionals.get(parameterName);
				needsOptional = true;
			}

			var type = typeFactory.select(parameterType);
			try {
				//OptionalColumnではなく、SELECT句にカラムがない場合、値はnull
				var value = (!isOptionalColumn || resultSetColumns.contains(parameterName.toUpperCase())) ? type.get(rs, parameterName) : null;
				parameters[i] = needsOptional ? Optional.ofNullable(value) : value;
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		try {
			var constructor = resultClass.getConstructor(parameterTypes);
			return constructor.newInstance(parameters);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}

	private Object createDefaultConstructorClassDataObject(ResultSet rs) {
		var tooManyColumnsDataObject = resultClass.getAnnotation(TooManyColumnsDataObject.class);

		Class<?> dataObjectClass;

		if (tooManyColumnsDataObject == null) {
			dataObjectClass = resultClass;
		} else {
			dataObjectClass = tooManyColumnsDataObject.bean();
		}

		Object object;

		try {
			var constructor = dataObjectClass.getConstructor();
			object = constructor.newInstance();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}

		var resultSetColumns = columnNamesFrom(rs);

		var optionals = new Optionals();
		Arrays.stream(resultClass.getFields()).forEach(f -> {
			var modifiers = f.getModifiers();
			//フィールドがstaticの場合は対象から除外
			//publicではない、finalの場合は以降の処理でエラーを起こすことで使用出来ないことを通知する
			if (Modifier.isStatic(modifiers)) return;

			var fieldName = f.getName();
			var fieldType = f.getType();

			var needsOptional = false;
			if (Optional.class.equals(fieldType)) {
				fieldType = optionals.get(fieldName);
				needsOptional = true;
			}

			var type = typeFactory.select(fieldType);

			var isOptionalColumn = f.getAnnotation(OptionalColumn.class) != null;

			try {
				//OptionalColumnではなく、SELECT句にカラムがない場合、値はnull
				var value = (!isOptionalColumn || resultSetColumns.contains(fieldName.toUpperCase())) ? type.get(rs, fieldName) : null;
				f.set(object, needsOptional ? Optional.ofNullable(value) : value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		});

		if (tooManyColumnsDataObject == null) {
			return object;
		}

		try {
			var constructor = resultClass.getConstructor(tooManyColumnsDataObject.bean());
			return constructor.newInstance(object);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}

	private class Optionals {

		Map<String, Class<?>> map;

		private Class<?> get(String name) {
			if (map == null) {
				map = new HashMap<>();

				Arrays.stream(loadOptionalDatas().value()).forEach(d -> map.put(d.name(), d.type()));
			}

			return map.get(name);
		}
	}

	private OptionalDatas loadOptionalDatas() {
		try {
			return Class.forName(
				resultClass.getName() + Constants.DATA_OBJECT_METADATA_CLASS_SUFFIX,
				true,
				Thread.currentThread().getContextClassLoader()).getAnnotation(OptionalDatas.class);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	void logElapsed(long startNanos) {
		AtomSql.logElapsed(sqlLogger, startNanos);
	}

	@Override
	public void setValues(PreparedStatement ps, Optional<StackTraceElement[]> stackTrace) throws SQLException {
		int[] i = { 1 };

		sql.placeholders(p -> i[0] = p.type().bind(i[0], ps, p.value()));

		sqlLogger.perform(logger -> {
			logger.log(Level.INFO, "------ SQL START ------");

			if (entry.name() != null) {
				logger.log(Level.INFO, "name: " + entry.name());
			}

			logger.log(Level.INFO, "call from:");

			for (var element : stackTrace.get()) {
				var elementString = element.toString();

				//無名モジュールから呼ばれた場合、moduleNameはnull
				//Atom SQLモジュール名に前方一致するものは、Atom SQL関連ソースとして除外する
				if ((AtomSql.moduleName != null && elementString.startsWith(AtomSql.moduleName))
					|| elementString.startsWith("java.") //java.で始まるモジュール名は除外
					|| elementString.contains("(Unknown Source)")
					|| elementString.contains("<generated>"))
					continue;

				if (AtomSql.configure().logStackTracePattern().matcher(elementString).find())
					logger.log(Level.INFO, " " + elementString);
			}

			var placeholders = sql.placeholders();

			if (placeholders.stream().filter(p -> p.confidential()).findFirst().isPresent()) {
				var bindingValues = placeholders.stream().map(p -> {
					String value;
					if (p.confidential()) {
						value = Constants.CONFIDENTIAL;
					} else {
						value = AtomSqlUtils.toStringForBindingValue(p.value());
					}

					return new BindingValue(p.name().toString(), value);
				}).toList();

				entry.endpoint()
					.logConfidentialSql(
						logger,
						sql.originalString(),
						sql.string(),
						bindingValues,
						snapshot);
			} else {
				entry.endpoint()
					.logSql(
						logger,
						sql.originalString(),
						sql.string(),
						ps,
						snapshot);
			}

			logger.log(Level.INFO, "------  SQL END  ------");
		});
	}

	private static record TypeAndArg(AtomSqlType type, Object arg) {}

}
