package io.github.tezch.atomsql;

import java.lang.System.Logger.Level;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.tezch.atomsql.AtomSql.SqlProxyHelper;
import io.github.tezch.atomsql.Endpoint.BindingValue;
import io.github.tezch.atomsql.annotation.DataObject;
import io.github.tezch.atomsql.annotation.OptionalColumn;
import io.github.tezch.atomsql.annotation.Sql;
import io.github.tezch.atomsql.annotation.SqlProxy;
import io.github.tezch.atomsql.annotation.processor.Methods;
import io.github.tezch.atomsql.annotation.processor.OptionalDatas;
import io.github.tezch.atomsql.annotation.processor.TooManyColumnsDataObject;

/**
 * {@link SqlProxy}が生成する中間形態オブジェクトを表すクラスです。<br>
 * 内部にSQL文を保持しており、その実行、またSQL文が断片だった場合は断片同士の結合等を行うことが可能です。<br>
 * 外部からの文字列を取り込まないためにシリアライズ不可
 * @author tezch
 * @param <T> {@link DataObject}が付与された型
 */
public class Atom<T> {

	/**
	 * 空文字列
	 */
	public static final Atom<?> EMPTY = newInstance("");

	/**
	 * 空白
	 */
	public static final Atom<?> BLANK = newInstance(" ");

	/**
	 * ,
	 */
	public static final Atom<?> COMMA = newInstance(", ");

	/**
	 * ,<br>
	 * COMMAの短縮形
	 */
	public static final Atom<?> C = COMMA;

	/**
	 * 改行
	 */
	public static final Atom<?> CRLF = newInstance("\r\n");

	/**
	 * INNER JOIN
	 */
	public static final Atom<?> INNER_JOIN = newInstance("INNER JOIN");

	/**
	 * LEFT OUTER JOIN
	 */
	public static final Atom<?> LEFT_OUTER_JOIN = newInstance("LEFT OUTER JOIN");

	/**
	 * RIGHT OUTER JOIN
	 */
	public static final Atom<?> RIGHT_OUTER_JOIN = newInstance("RIGHT OUTER JOIN");

	/**
	 * WHERE
	 */
	public static final Atom<?> WHERE = newInstance("WHERE");

	/**
	 * HAVING
	 */
	public static final Atom<?> HAVING = newInstance("HAVING");

	/**
	 * ORDER BY
	 */
	public static final Atom<?> ORDER_BY = newInstance("ORDER BY");

	/**
	 * ASC
	 */
	public static final Atom<?> ASC = newInstance("ASC");

	/**
	 * DESC
	 */
	public static final Atom<?> DESC = newInstance("DESC");

	/**
	 * GROUP BY
	 */
	public static final Atom<?> GROUP_BY = newInstance("GROUP BY");

	/**
	 * (
	 */
	public static final Atom<?> LEFT_PAREN = newInstance("(");

	/**
	 * (<br>
	 * LEFT_PARENの短縮形
	 */
	public static final Atom<?> L = LEFT_PAREN;

	/**
	 * )
	 */
	public static final Atom<?> RIGHT_PAREN = newInstance(")");

	/**
	 * )<br>
	 * RIGHT_PARENの短縮形
	 */
	public static final Atom<?> R = RIGHT_PAREN;

	/**
	 * AND
	 */
	public static final Atom<?> AND = newInstance("AND");

	/**
	 * OR
	 */
	public static final Atom<?> OR = newInstance("OR");

	private static final SecureString leftParen = new SecureString("(");

	private static final SecureString rightParen = new SecureString(")");

	private final AtomSql atomSql;

	private final SqlProxyHelper helper;

	private final Supplier<SqlComposite> sqlSupplier;

	private final PreparedStatementSetter preparedStatementSetter;

	private final Object nonThreadSafeSqlKey = new Object();

	private final boolean andType;

	Atom(
		AtomSql atomsql,
		SqlProxyHelper helper,
		SqlComposite sql,
		boolean andType) {
		this.atomSql = atomsql;
		this.helper = helper;

		if (sql.containsNonThreadSafeValue) {
			atomSql.registerSqlCompositeForNonThreadSafe(nonThreadSafeSqlKey, sql);
			this.sqlSupplier = () -> atomSql.getHelperForNonThreadSafe(nonThreadSafeSqlKey);
		} else {
			this.sqlSupplier = () -> sql;
		}

		this.andType = andType;

		preparedStatementSetter = createPreparedStatementSetter();
	}

	private Atom(
		AtomSql atomsql,
		SqlProxyHelper helper,
		Supplier<SqlComposite> sqlSupplier,
		boolean andType) {
		this.atomSql = atomsql;
		this.helper = helper;
		this.sqlSupplier = sqlSupplier;
		this.andType = andType;
		preparedStatementSetter = createPreparedStatementSetter();
	}

	@SuppressWarnings("unchecked")
	private RowMapper<T> dataObjectCreator() {
		return (r, n) -> (T) createDataObject(r);
	}

	private static Atom<?> newInstance(String sql) {
		var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();

		if (!caller.equals(Atom.class))
			throw new SecurityException("Direct access not allowed");

		var atomSql = new AtomSql();
		return new Atom<>(
			atomSql,
			atomSql.helper(),
			atomSql.sqlComposite(new SecureString(sql)),
			true);
	}

	SqlProxyHelper helper() {
		return helper;
	}

	SqlComposite sqlComposite() {
		return sqlSupplier.get();
	}

	PreparedStatementSetter preparedStatementSetter() {
		return preparedStatementSetter;
	}

	private static class Holder<T> {

		private T value;

		private T get() {
			return value;
		}

		private void set(T value) {
			this.value = value;
		}
	}

	Object createDataObject(ResultSet rs) {
		var resultClass = helper.resultClass();
		var typeFactory = helper.typeFactory();

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
		var resultClass = helper.resultClass();

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

			var type = helper.typeFactory().select(parameterType);
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
		var resultClass = helper.resultClass();

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

			var type = helper.typeFactory().select(fieldType);

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
				helper.resultClass().getName() + Constants.DATA_OBJECT_METADATA_CLASS_SUFFIX,
				true,
				Thread.currentThread().getContextClassLoader()).getAnnotation(OptionalDatas.class);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	void logElapsed(long startNanos) {
		AtomSql.logElapsed(helper.sqlLogger(), startNanos);
	}

	/**
	 * {@link Atom}の簡易生成メソッドです。<br>
	 * このメソッドで生成されたインスタンスを、このメソッドを使用したクラスのstaticフィールドに格納する必要があります。<br>
	 * {@link Atom}は、格納されたフィールドに付与された{@link Sql}またはフィールド名の値を持ちます。<br>
	 * 生成されたインスタンスはスレッドセーフであり、static変数に保存し使用することが可能です。<br>
	 * このメソッドで生成されたインスタンスでは、検索等のデータベース操作を行うことはできません。<br>
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。<br>
	 * @return {@link Atom}
	 */
	public static Atom<?> of() {
		var atomSql = new AtomSql();

		var atomHolder = new Holder<Atom<?>>();

		var sqlHolder = new Holder<SqlComposite>();

		var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();

		var atom = new Atom<>(atomSql, atomSql.helper(), () -> {
			synchronized (sqlHolder) {
				var helper = sqlHolder.get();
				if (helper != null) return helper;

				var sql = Arrays.stream(caller.getDeclaredFields())
					.filter(f -> {
						f.setAccessible(true);

						try {
							synchronized (atomHolder) {
								return atomHolder.get() == f.get(null);
							}
						} catch (Exception e) {
							e.printStackTrace();
							return false;
						}
					})
					.findFirst()
					.map(Atom::extractForm)
					//自分自身が見つからなかった
					.orElseThrow(() -> new IllegalStateException("Atom must be a static field in the calling class"));

				helper = atomSql.sqlComposite(new SecureString(sql));

				sqlHolder.set(helper);

				return helper;
			}
		}, true);

		synchronized (atomHolder) {
			atomHolder.set(atom);
		}

		return atom;
	}

	/**
	 * フィールドに付与された{@link Sql}またはフィールド名からインスタンスを生成します。<br>
	 * このメソッドは呼び出されるたびに新たに{@link Atom}のインスタンスを生成するので、使用する側で適宜キャッシュ等を行ってください。<br>
	 * 生成されたインスタンスはスレッドセーフであり、static変数に保存し使用することが可能です。<br>
	 * このメソッドで生成されたインスタンスでは、検索等のデータベース操作を行うことはできません。<br>
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。<br>
	 * また、このメソッドを呼ぶ前に初期化が完了している必要があります。
	 * @param field {@link Field}
	 * @return {@link Atom}
	 */
	public static Atom<?> of(Field field) {
		return newInstance(extractForm(field));
	}

	private static String extractForm(Field field) {
		var sql = field.getAnnotation(Sql.class);

		if (sql != null)
			return sql.value();

		return field.getName();
	}

	/**
	 * enumの要素に付与された{@link Sql}または要素名からインスタンスを生成します。<br>
	 * このメソッドは呼び出されるたびに新たに{@link Atom}のインスタンスを生成するので、使用する側で適宜キャッシュ等を行ってください。<br>
	 * 生成されたインスタンスはスレッドセーフであり、static変数に保存し使用することが可能です。<br>
	 * このメソッドで生成されたインスタンスでは、検索等のデータベース操作を行うことはできません。<br>
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。<br>
	 * また、このメソッドを呼ぶ前に初期化が完了している必要があります。
	 * @param enumConstant enumの要素
	 * @return {@link Atom}
	 */
	public static <E extends Enum<E>> Atom<?> of(Enum<E> enumConstant) {
		try {
			return of(enumConstant.getDeclaringClass().getDeclaredField(enumConstant.name()));
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Atom結合用のインスタンスを生成します。<br>
	 * 生成されたインスタンスはスレッドセーフであり、static変数に保存し使用することが可能です。<br>
	 * このメソッドで生成されたインスタンスでは、検索等のデータベース操作を行うことはできません。<br>
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。<br>
	 * また、このメソッドを呼ぶ前に初期化が完了している必要があります。
	 * @see AtomSql#initialize(Configure)
	 * @param creator {@link Function}
	 * @return creator内で生成されたインスタンス
	 */
	public static Atom<?> of(Function<AtomSql, Atom<?>> creator) {
		return creator.apply(new AtomSql());
	}

	/**
	 * 検索結果を{@link Stream}として返します。<br>
	 * 内部的に{@link ResultSet}を使用して逐次行取得しており、明示的に{@link Stream#close()}するまで{@link ResultSet}が閉じられないので注意が必要です。<br>
	 * 検索結果全件に対して操作を行いたい（{@link Stream#map}等）、結果オブジェクトすべてが必要でない場合は{@link List}で結果取得するよりも若干効率的です。
	 * @see AtomSql#tryStream(Runnable)
	 * @return {@link DataObject}付与結果オブジェクトの{@link Stream}
	 */
	public Stream<T> stream() {
		return stream(dataObjectCreator());
	}

	/**
	 * 検索結果を{@link List}として返します。<br>
	 * 内部的に{@link ResultSet}から全件結果取得してから{@link List}として返却しています。<br>
	 * @return {@link DataObject}付与結果オブジェクトの{@link List}
	 */
	public List<T> list() {
		return listAndClose(streamInternal(dataObjectCreator()));
	}

	/**
	 * 検索結果が1件であることが判明している場合（PKを条件に検索した場合等）、このメソッドを使用することでその1件のみ取得することが可能です。<br>
	 * 検索結果が0件の場合、空の{@link Optional}が返されます。<br>
	 * 検索結果が1件以上ある場合、不正な状態であると判断し例外がスローされます。
	 * @return {@link DataObject}付与型の結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public Optional<T> get() {
		return get(list());
	}

	/**
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #stream()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@link DataObject}付与結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(Class<R> dataObjectClass) {
		return apply(dataObjectClass).stream();
	}

	/**
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #list()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@link DataObject}付与結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(Class<R> dataObjectClass) {
		return apply(dataObjectClass).list();
	}

	/**
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #get()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@link DataObject}付与型の結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public <R> Optional<R> get(Class<R> dataObjectClass) {
		return apply(dataObjectClass).get();
	}

	/**
	 * データオブジェクトの型を持たない{@link Atom}から、データオブジェクトの型を与えたインスタンスを新たに生成します。
	 * @param <R> {@link DataObject}が付与された型
	 * @param dataObjectClass {@link DataObject}が付与されたクラスオブジェクト
	 * @return データオブジェクトの型が与えられた新インスタンス
	 */
	public <R> Atom<R> apply(Class<R> dataObjectClass) {
		if (dataObjectClass.getAnnotation(DataObject.class) == null)
			throw new IllegalStateException("dataObjectClass required annotation @" + DataObject.class.getSimpleName());

		return new Atom<R>(
			atomSql,
			SqlProxyHelper.newHelper(
				helper,
				dataObjectClass),
			sqlComposite(),
			andType);
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link Stream}として返します。
	 * @see #stream
	 * @see AtomSql#tryStream(Runnable)
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(RowMapper<R> mapper) {
		var stream = streamInternal(mapper);
		atomSql.registerStream(stream);
		return stream;
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(RowMapper<R> mapper) {
		return listAndClose(streamInternal(mapper));
	}

	private <R> Stream<R> streamInternal(RowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var startNanos = System.nanoTime();
		try {
			return helper.entry()
				.endpoint()
				.queryForStream(
					sqlComposite().compiled().sqlString(),
					preparedStatementSetter,
					mapper,
					helper.snapshot());
		} finally {
			logElapsed(startNanos);
		}
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link Stream}として返します。<br>
	 * @see #stream
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(SimpleRowMapper<R> mapper) {
		return stream((r, n) -> mapper.mapRow(r));
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(SimpleRowMapper<R> mapper) {
		return listAndClose(streamInternal((r, n) -> mapper.mapRow(r)));
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクト一件を{@link Optional}にラップして返します。<br>
	 * @see #get
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public <R> Optional<R> get(SimpleRowMapper<R> mapper) {
		return get(list(mapper));
	}

	/**
	 * このインスタンスが持つ?プレースホルダ変換前のSQL文もしくはその一部を返します。
	 * @return SQL文もしくはその一部
	 */
	public String sql() {
		return sqlComposite().compiled().sqlString();
	}

	/**
	 * @see Atom#sql
	 * @return SQL文もしくはその一部
	 */
	@Override
	public String toString() {
		return sqlComposite().originalString();
	}

	/**
	 * 内部に持つSQLが空文字列かどうかを返します。
	 * @return 内部に持つSQLが空文字列である場合、true
	 */
	public boolean isEmpty() {
		return sql().isEmpty();
	}

	private <E> Optional<E> get(List<E> list) {
		if (list.size() > 1)
			//結果は1行以下でなければなりません
			throw new IllegalStateException("The result must be less than or equal to one row");

		return list.stream().findFirst();
	}

	/**
	 * 更新処理（INSERT, UPDATE, DELETE）のDML文、DDL文を実行します。<br>
	 * DDL、バッチ実行の場合、結果は常に0となります。
	 * @return 更新処理の場合、その結果件数
	 */
	public int execute() {
		var entry = helper.entry();

		var resources = atomSql.batchResources();
		if (resources == null) {//バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				return entry.endpoint()
					.update(
						sqlComposite().compiled().sqlString(),
						preparedStatementSetter,
						helper.snapshot());
			} finally {
				logElapsed(startNanos);
			}
		}

		resources.put(entry.name(), this, null, AtomSqlUtils.stackTrace());

		return 0;
	}

	/**
	 * 更新処理（INSERT, UPDATE, DELETE）のDML文、DDL文を実行します。<br>
	 * バッチ更新であっても処理結果件数はresultListenerに通知されます。<br>
	 * バッチ更新の場合、resultListener内で例外を投げると、未実行のバッチ更新が失われるため、途中から更新を再開することは出来ません。
	 * @param resultConsumer 
	 * @see AtomSql#tryBatch(Runnable)
	 * @see AtomSql#tryBatch(Supplier)
	 */
	public void execute(Consumer<Integer> resultConsumer) {
		var entry = helper.entry();

		var resources = atomSql.batchResources();
		if (resources == null) {//バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				resultConsumer.accept(
					entry.endpoint()
						.update(
							sqlComposite().compiled().sql().toString(),
							preparedStatementSetter,
							helper.snapshot()));

				return;
			} finally {
				logElapsed(startNanos);
			}
		}

		resources.put(
			entry.name(),
			this,
			Objects.requireNonNull(resultConsumer),
			AtomSqlUtils.stackTrace());
	}

	/**
	 * {@link Atom}が内部で持つバインドされた値を新たに再バインドし、新しいインスタンスとして返します。<br>
	 * valuesに存在しないプレースホルダ名の値は既存の値が使用されます。<br>
	 * 値にnullを使用したい場合はキーがプレースホルダ名、値がnullとしてください。
	 * @param values キーはプレースホルダ名、値は再バインドする値
	 * @return 新たに作成された{@link Atom}
	 */
	public Atom<T> rebind(Map<String, Object> values) {
		return new Atom<T>(
			atomSql,
			helper,
			SqlComposite.rebind(sqlComposite(), Objects.requireNonNull(values)),
			andType);
	}

	/**
	 * 内部に持つSQL文の一部同士を" "をはさんで文字列結合します。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。<br>
	 * 複数を一度に結合することが可能です。
	 * @param others 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> fuse(Atom<?>... others) {
		var result = this;
		for (var another : others) {
			result = result.fuseWithInternal(SqlComposite.BLANK, another);
		}

		return result;
	}

	/**
	 * 内部に持つSQL文を、自身を先頭にdelimiterをはさんで文字列結合します。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。<br>
	 * @param delimiter 区切り文字列
	 * @param others 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> fuseWith(Atom<?> delimiter, Atom<?>... others) {
		var result = this;
		var delimiterPart = delimiter.sqlComposite();
		for (var another : others) {
			result = result.fuseWithInternal(delimiterPart, another);
		}

		return result;
	}

	private Atom<T> fuseWithInternal(SqlComposite delimiter, Atom<?> another) {
		Objects.requireNonNull(another);

		var sql = concat(delimiter, sqlComposite(), another.sqlComposite());
		return new Atom<T>(
			atomSql,
			helper,
			sql,
			true);
	}

	/**
	 * 複数の{@link Atom}を、delimiterをはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param delimiter 区切り文字
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> interfuse(Atom<?> delimiter, List<Atom<?>> members) {
		return members.size() == 0
			? EMPTY
			: members.get(0).fuseWith(delimiter, members.subList(1, members.size()).toArray(Atom<?>[]::new));
	}

	/**
	 * 複数の{@link Atom}を、", "をはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> withBlank(Atom<?>... members) {
		return members.length == 0
			? EMPTY
			: members[0].fuseWith(BLANK, Arrays.copyOfRange(members, 1, members.length));
	}

	/**
	 * 複数の{@link Atom}を、", "をはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> withComma(Atom<?>... members) {
		return members.length == 0
			? EMPTY
			: members[0].fuseWith(COMMA, Arrays.copyOfRange(members, 1, members.length));
	}

	/**
	 * 複数の{@link Atom}を、", "をはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> withBlank(List<Atom<?>> members) {
		return interfuse(BLANK, members);
	}

	/**
	 * 複数の{@link Atom}を、", "をはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> withComma(List<Atom<?>> members) {
		return interfuse(COMMA, members);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで渡される{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>配列atomsのインデックス番号</i>}*&#47; です。<br>
	 * 例えば、展開対象1番目のための変数は /*${0}*&#47; と記述します。
	 * @param atoms 展開する{@link Atom}の配列
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> implant(Atom<?>... atoms) {
		var sql = sqlComposite();

		for (int i = 0; i < atoms.length; i++) {
			var atom = atoms[i];

			var pattern = pattern(String.valueOf(i));
			sql = sql.replace(pattern, atom.sqlComposite());
		}

		return new Atom<T>(atomSql, helper, sql, true);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで回収する{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。<br>
	 * キーワードのスペルミスを防ぎたい場合は、{@link Protoatom}機能の使用を検討してください。
	 * @param keyword 変数名
	 * @param atom 展開する{@link Atom}
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> implant(String keyword, Atom<?> atom) {
		var sql = sqlComposite();

		var pattern = pattern(Objects.requireNonNull(keyword));

		return new Atom<T>(atomSql, helper, sql.replace(pattern, atom.sqlComposite()), true);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで回収する{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。<br>
	 * キーワードのスペルミスを防ぎたい場合は、{@link Protoatom}機能の使用を検討してください。
	 * @param atoms 変数名をキー、{@link Atom}を値として格納したマップ
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> implant(Map<String, Atom<?>> atoms) {
		var sql = new SqlComposite[] { sqlComposite() };

		atoms.entrySet().stream().forEach(e -> {
			var atom = e.getValue();

			var pattern = pattern(Objects.requireNonNull(e.getKey()));
			sql[0] = sql[0].replace(pattern, atom.sqlComposite());
		});

		return new Atom<T>(atomSql, helper, sql[0], true);
	}

	private static Pattern pattern(String keyword) {
		return Pattern.compile("/\\*\\$\\{" + keyword + "\\}\\*/");
	}

	/**
	 * 内部に持つSQL文の一部同士を" AND "をはさんで文字列結合します。<br>
	 * このインスタンスかもう一方のもつSQLが空の場合、結合は行われず、SQLが空ではない側のインスタンスが返されます。<br>
	 * このインスタンスかもう一方のもつSQLが既に{@link #or}を使用して結合されたものであった場合、OR側のSQLは外側に()が付与され保護されます。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。
	 * @param another 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> and(Atom<?> another) {
		return andOr(SqlComposite.AND, another, true);
	}

	/**
	 * 内部に持つSQL文の一部同士を" OR "をはさんで文字列結合します。<br>
	 * このインスタンスかもう一方のもつSQLが空の場合、結合は行われず、SQLが空ではない側のインスタンスが返されます。<br>
	 * {@link #or}を使用して結合されたインスタンスを{@link #and}を使用して結合した場合、このインスタンスのSQLは外側に()が付与され保護されます。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。
	 * @param another 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> or(Atom<?> another) {
		//どちらか一方でも空の場合OR結合が発生しないのでAND状態のままとする
		var andType = isEmpty() || another.isEmpty();
		return andOr(SqlComposite.OR, another, andType);
	}

	private Atom<T> andOr(SqlComposite delimiter, Atom<?> another, boolean andTypeCurrent) {
		Objects.requireNonNull(another);

		var sql = guardSql(andType, andTypeCurrent, this);
		var anotherSql = guardSql(another.andType, andTypeCurrent, another);

		return new Atom<T>(
			atomSql,
			helper,
			concat(delimiter, sql, anotherSql),
			andTypeCurrent);
	}

	private static SqlComposite concat(SqlComposite delimiter, SqlComposite sql1, SqlComposite sql2) {
		if (sql1.isBlank()) return sql2;
		if (sql2.isBlank()) return sql1;

		return sql1.concat(delimiter).concat(sql2);
	}

	private static SqlComposite guardSql(boolean andType, boolean andTypeCurrent, Atom<?> atom) {
		var sql = atom.sqlComposite();

		if (!andType && andTypeCurrent) {//現在ORでAND追加された場合
			return sql.isBlank() ? SqlComposite.EMPTY : sql.join(leftParen, rightParen);
		}

		return sql;
	}

	private static <T> List<T> listAndClose(Stream<T> stream) {
		try (stream) {
			return stream.toList();
		}
	}

	private PreparedStatementSetter createPreparedStatementSetter() {
		return new PreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, Optional<StackTraceElement[]> stackTrace) throws SQLException {
				int[] i = { 1 };

				var sqlComposite = sqlComposite();

				var sql = sqlComposite.compiled();

				sql.placeholders().forEach(p -> i[0] = p.type().bind(i[0], ps, p.value()));

				helper.sqlLogger().perform(logger -> {
					var entry = helper.entry();

					var snapshot = helper.snapshot();

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
								sqlComposite.originalString(),
								sql.sqlString(),
								bindingValues,
								snapshot);
					} else {
						entry.endpoint()
							.logSql(
								logger,
								sqlComposite.originalString(),
								sql.sqlString(),
								ps,
								snapshot);
					}

					logger.log(Level.INFO, "------  SQL END  ------");
				});
			}
		};
	}
}
