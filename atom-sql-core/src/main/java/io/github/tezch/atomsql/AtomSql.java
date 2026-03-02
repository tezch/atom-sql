package io.github.tezch.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.tezch.atomsql.SqlComposite.Component;
import io.github.tezch.atomsql.SqlComposite.Prototype;
import io.github.tezch.atomsql.SqlComposite.SqlCompositeHelper;
import io.github.tezch.atomsql.SqlComposite.Text;
import io.github.tezch.atomsql.annotation.Confidential;
import io.github.tezch.atomsql.annotation.ConfidentialSql;
import io.github.tezch.atomsql.annotation.NoSqlLog;
import io.github.tezch.atomsql.annotation.NonThreadSafe;
import io.github.tezch.atomsql.annotation.Qualifier;
import io.github.tezch.atomsql.annotation.Sql;
import io.github.tezch.atomsql.annotation.SqlFile;
import io.github.tezch.atomsql.annotation.SqlProxy;
import io.github.tezch.atomsql.annotation.processor.Methods;
import io.github.tezch.atomsql.type.NULL;

/**
 * Atom SQLの実行時の処理のほとんどを行うコアクラスです。<br>
 * {@link SqlProxy}の生成および更新処理のバッチ実行の操作が可能です。
 * @author tezch
 */
public class AtomSql {

	/**
	 * AtomSqlにパラメーターの設定を適用して初期化します。
	 * @param config 設定
	 * @throws IllegalStateException 既に初期化済みの場合
	 */
	public static void initialize(Configure config) {
		AtomSqlInitializer.initialize(config);
	}

	/**
	 * AtomSqlにデフォルトの設定を適用して初期化します。
	 * @throws IllegalStateException 既に初期化済みの場合
	 */
	public static void initialize() {
		AtomSqlInitializer.initialize();
	}

	/**
	 * AtomSqlにパラメーターの設定を適用して初期化します。<br>
	 * 既に初期化済みの場合このメソッドは何も行いません。
	 * @param config 設定
	 */
	public static void initializeIfUninitialized(Configure config) {
		AtomSqlInitializer.initializeIfUninitialized(config);
	}

	/**
	 * AtomSqlにデフォルトの設定を適用して初期化します。<br>
	 * 既に初期化済みの場合このメソッドは何も行いません。
	 */
	public static void initializeIfUninitialized() {
		AtomSqlInitializer.initializeIfUninitialized();
	}

	/**
	 * 現設定値
	 * @return {@link Configure}
	 */
	public static Configure configure() {
		return AtomSqlInitializer.configure();
	}

	static final Logger logger = System.getLogger(AtomSql.class.getName());

	static final String moduleName = AtomSql.class.getModule().getName();

	private final AtomSqlTypeFactory typeFactory;

	private final SqlLogger sqlLogger;

	private static final ThreadLocal<Map<Object, SqlComposite>> nonThreadSafeSqls = new ThreadLocal<>();

	private final ThreadLocal<BatchResources> batchResources = new ThreadLocal<>();

	private final ThreadLocal<List<Stream<?>>> streams = new ThreadLocal<>();

	private final Endpoints endpoints;

	final Optional<Pattern> logStacktracePattern;

	class BatchResources {

		private static record Resource(
			Atom<?> atom,
			Consumer<Integer> resultConsumer,
			Optional<StackTraceElement[]> stackTrace) {}

		private final Map<String, Map<String, List<Resource>>> allResources = new HashMap<>();

		private final int threshold;

		private int num = 0;

		private BatchResources() {
			var threshold = configure().batchThreshold();
			this.threshold = threshold > 0 ? threshold : Integer.MAX_VALUE;
		}

		void put(String name, Atom<?> atom, Consumer<Integer> resultConsumer, Optional<StackTraceElement[]> stackTrace) {
			if (num == threshold) flushAll();

			allResources.computeIfAbsent(name, n -> new HashMap<>())
				.computeIfAbsent(
					atom.sqlComposite().compiled().sqlString(),
					s -> new ArrayList<>())
				.add(new Resource(atom, resultConsumer, stackTrace));
			num++;
		}

		private void flushAll() {
			try {
				allResources.forEach((name, map) -> {
					map.forEach((sql, resources) -> {
						flush(name, sql, resources);
					});
				});
			} finally {
				//flush内で例外が発生した場合、tryBatch等のfinallyでflushAllが実施されるため、二度実行されないように必ず空にする
				num = 0;
				allResources.clear();
			}
		}

		private void flush(String name, String sql, List<Resource> resources) {
			var startNanos = System.nanoTime();
			try {
				var results = endpoints.get(name).endpoint().batchUpdate(sql, new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						var resource = resources.get(i);
						resource.atom.preparedStatementSetter().setValues(ps, resource.stackTrace);
					}

					@Override
					public SqlProxySnapshot sqlProxySnapshot(int i) {
						return resources.get(i).atom.helper().snapshot();
					}

					@Override
					public int getBatchSize() {
						int size = resources.size();

						sqlLogger.perform(logger -> logger.log(Level.INFO, "batch size: " + size));

						return size;
					}
				});

				for (var i = 0; i < results.length; i++) {
					var resultConsumer = resources.get(i).resultConsumer;
					if (resultConsumer != null) resultConsumer.accept(results[i]);
				}
			} finally {
				logElapsed(sqlLogger, startNanos);
			}
		}
	}

	/**
	 * 通常のコンストラクタです。<br>
	 * {@link Endpoints}の持つ{@link Endpoint}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param endpoints {@link Endpoints}
	 */
	public AtomSql(Endpoints endpoints) {
		typeFactory = AtomSqlTypeFactory.newInstance(
			configure().typeFactoryClass(),
			Thread.currentThread().getContextClassLoader());
		sqlLogger = SqlLogger.instance();
		this.endpoints = Objects.requireNonNull(endpoints);
		logStacktracePattern = logStacktracePattern(configure());
	}

	/**
	 * コピーコンストラクタです。<br>
	 * baseと同じ接続先をもつ別インスタンスが生成されます。<br>
	 * バッチの実施単位を分けたい場合などに使用します。
	 * @param base コピー元
	 */
	public AtomSql(AtomSql base) {
		typeFactory = base.typeFactory;
		sqlLogger = base.sqlLogger;
		this.endpoints = base.endpoints;
		logStacktracePattern = logStacktracePattern(configure());
	}

	AtomSql() {
		var configure = configure();

		typeFactory = AtomSqlTypeFactory.newInstance(
			configure.typeFactoryClass(),
			Thread.currentThread().getContextClassLoader());
		sqlLogger = SqlLogger.instance();

		endpoints = new Endpoints(new Endpoint() {

			@Override
			public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> Stream<T> queryForStream(
				String sql,
				PreparedStatementSetter pss,
				RowMapper<T> rowMapper,
				SqlProxySnapshot snapshot) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int update(String sql, PreparedStatementSetter pss, SqlProxySnapshot snapshot) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void logSql(
				Logger logger,
				String originalSql,
				String sql,
				PreparedStatement ps,
				SqlProxySnapshot snapshot) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void logConfidentialSql(
				Logger logger,
				String originalSql,
				String sql,
				List<BindingValue> bindingValues,
				SqlProxySnapshot snapshot) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void bollowConnection(Consumer<ConnectionProxy> consumer) {
				throw new UnsupportedOperationException();
			}
		});

		logStacktracePattern = logStacktracePattern(configure);
	}

	private static Optional<Pattern> logStacktracePattern(Configure configure) {
		if (configure.enableLog()) {
			return Optional.of(Pattern.compile(configure.logStacktracePattern()));
		}

		return Optional.empty();
	}

	/**
	 * 設定で{@link Qualifier}を使用するとされている場合、対象に付与された{@link Qualifier}を返す<br>
	 * 対象自体に{@link Qualifier}が無くても、その他のアノテーション自体に{@link Qualifier}が付与されていればそれを返す
	 */
	private Optional<Qualifier> qualifier(AnnotatedElement e) {
		if (!configure().usesQualifier()) return Optional.empty();

		var qualifier = e.getAnnotation(Qualifier.class);
		if (qualifier != null) return Optional.of(qualifier);

		var annotations = e.getAnnotations();

		if (annotations.length == 0) return Optional.empty();

		return Arrays.stream(annotations)
			.map(a -> a.annotationType().getAnnotation(Qualifier.class))
			.filter(q -> q != null)
			.findFirst();
	}

	/**
	 * {@link SqlProxy}が付与されたインターフェイスから{@link Proxy}オブジェクトを作成します。
	 * @see Proxy
	 * @see SqlProxy
	 * @param <T> 生成される{@link Proxy}の型
	 * @param proxyInterface {@link SqlProxy}が付与されたインターフェイス
	 * @return 生成された{@link Proxy}
	 * @throws IllegalArgumentException proxyInterfaceがインターフェイスではない場合
	 * @throws IllegalArgumentException proxyInterfaceに{@link SqlProxy}が付与されていない場合
	 */
	public <T> T of(Class<T> proxyInterface) {
		if (!proxyInterface.isInterface())
			//proxyInterfaceはインターフェイスではありません
			throw new IllegalArgumentException(proxyInterface + " is not interface");

		if (!proxyInterface.isAnnotationPresent(SqlProxy.class))
			//アノテーションSqlProxyが見つかりません
			throw new IllegalArgumentException("Annotation " + SqlProxy.class.getSimpleName() + " is not found");

		@SuppressWarnings("unchecked")
		T instance = (T) Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class<?>[] { proxyInterface },
			this::invokeMethod);

		return instance;
	}

	/**
	 * このインスタンスが持つ{@link SqlProxy}情報のキャッシュをクリアします。
	 */
	public void clearCache() {
		synchronized (cache) {
			cache.clear();
		}
	}

	private final int capacity = configure().cacheCapacity();

	private final ConcurrentHashMap<Method, Helpers> cache = cache();

	private ConcurrentHashMap<Method, Helpers> cache() {
		return capacity > 0 ? new ConcurrentHashMap<>() : null;
	}

	private Object invokeMethod(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);

		var returnType = method.getReturnType();

		if (returnType.equals(AtomSql.class)) return AtomSql.this;

		if (returnType.isAnnotationPresent(SqlProxy.class)) return of(returnType);

		var proxyInterface = proxy.getClass().getInterfaces()[0];

		var proxyName = proxyInterface.getName();

		var methods = Class.forName(
			proxyName + Constants.METADATA_CLASS_SUFFIX,
			true,
			Thread.currentThread().getContextClassLoader()).getAnnotation(Methods.class);

		var methodName = method.getName();

		var metadata = Arrays.stream(methods.value())
			.filter(
				m -> m.name().equals(methodName) && Arrays.equals(method.getParameterTypes(), m.parameterTypes()))
			.findFirst()
			.get();

		var parameterBinderClass = metadata.parameterBinder();

		// ParameterBinderを使用している場合、同じ処理を二度しないためスキャンした内容をhelper作成に引き渡す
		Optional<ParameterBinderInfo> parameterBinderInfo;
		Object[] computedValues;
		if (!parameterBinderClass.equals(Object.class)) {
			var parameterBinder = parameterBinderClass.getConstructor().newInstance();

			Consumer.class.getMethod("accept", Object.class).invoke(args[0], new Object[] { parameterBinder });

			var values = new LinkedList<Object>();

			var fields = Arrays.stream(parameterBinderClass.getFields()).toList();

			fields.forEach(f -> {
				Object value;
				try {
					value = f.get(parameterBinder);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}

				values.add(value);
			});

			computedValues = values.toArray(new Object[values.size()]);

			parameterBinderInfo = Optional.of(new ParameterBinderInfo(fields, values));
		} else {
			computedValues = args;

			parameterBinderInfo = Optional.empty();
		}

		Helpers helpers;
		if (cache == null) {//キャッシュを利用しない設定の場合
			helpers = helpers(proxyInterface, method, metadata, parameterBinderInfo).helpers;
		} else {
			Helpers[] helpersHolder = { null };
			helpers = cache.computeIfAbsent(method, m -> {
				var result = helpers(proxyInterface, method, metadata, parameterBinderInfo);

				//キャッシュするにしてもしなくてもここで回収
				helpersHolder[0] = result.helpers;

				//キャッシュに登録しない
				if (!result.canCache) return null;

				//キャッシュに登録
				return result.helpers;
			});

			if (helpers == null)
				helpers = helpersHolder[0];

			if (cache.size() > capacity) {
				//適当に1件削除
				var it = cache.keySet().iterator();
				if (it.hasNext()) {
					it.next();
					it.remove();
				}
			}
		}

		var atom = new Atom<Object>(
			AtomSql.this,
			helpers.sqlProxyHelper,
			SqlComposite.createSqlComposite(helpers.sqlCompositeHelper, computedValues),
			true);

		if (returnType.equals(Atom.class)) {
			return atom;
		} else if (returnType.equals(Stream.class)) {
			return atom.stream();
		} else if (returnType.equals(List.class)) {
			return atom.list();
		} else if (returnType.equals(Optional.class)) {
			return atom.get();
		} else if (returnType.equals(int.class) || returnType.equals(void.class)) {
			return atom.execute();
		} else if (returnType.equals(Protoatom.class)) {
			return new Protoatom<>(atom, metadata.protoatomImplanter());
		} else {
			//不正な戻り値の型
			throw new IllegalStateException("Incorrect return type: " + returnType);
		}
	}

	private static record ParameterBinderInfo(List<Field> fields, List<Object> values) {}

	private HelpersResult helpers(
		Class<?> proxyInterface,
		Method method,
		io.github.tezch.atomsql.annotation.processor.Method metadata,
		Optional<ParameterBinderInfo> parameterBinderInfo) {
		var parameterNames = metadata.parameters();

		var confidentialSql = method.getAnnotation(ConfidentialSql.class);

		Supplier<String[]> confidentialSqlValueSupplier = () -> {
			var confidentialSqlValue = confidentialSql.value();

			//ConfidentialSqlが付与されているが、valueが指定されていない場合、すべて機密扱い
			return confidentialSqlValue.length == 0 ? parameterNames : confidentialSqlValue;
		};

		var confidentials = new HashSet<>(Arrays.asList(confidentialSql == null ? emptyStringArray : confidentialSqlValueSupplier.get()));

		var parameterAnnotations = method.getParameterAnnotations();

		var parameterBinderClass = metadata.parameterBinder();

		for (int i = 0; i < parameterNames.length; i++) {
			var name = parameterNames[i];
			var annotations = parameterAnnotations[i];

			Arrays.stream(annotations)
				.filter(a -> a instanceof Confidential)
				.findFirst()
				.ifPresent(a -> {
					if (parameterBinderInfo.isPresent()) {
						Arrays.stream(parameterBinderClass.getFields()).map(f -> f.getName()).forEach(confidentials::add);
					} else {
						confidentials.add(name);
					}
				});
		}

		SecureString sql;
		try {
			sql = loadSql(proxyInterface, method);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var conf = configure();

		SqlLogger mySqlLogger;
		if (conf.enableLog()) {
			NoSqlLog noSqlLog;
			String noSqlLogSign;
			if ((noSqlLog = proxyInterface.getAnnotation(NoSqlLog.class)) != null) {
				noSqlLogSign = proxyInterface.toString();
			} else if ((noSqlLog = method.getAnnotation(NoSqlLog.class)) != null) {
				noSqlLogSign = method.toString();
			} else {
				noSqlLog = null;
				noSqlLogSign = null;
			}

			if (!conf.shouldIgnoreNoSqlLog() && noSqlLog != null) {
				if (noSqlLog.logElapseTime()) {
					mySqlLogger = SqlLogger.noSqlLogInstance(noSqlLogSign);
				} else {
					mySqlLogger = SqlLogger.disabled;
				}
			} else {
				mySqlLogger = sqlLogger;
			}
		} else {
			mySqlLogger = SqlLogger.disabled;
		}

		var snapshot = new SqlProxySnapshot() {

			@Override
			public String getClassName() {
				return proxyInterface.getName();
			}

			@Override
			public <T extends Annotation> T getClassAnnotation(Class<T> annotationClass) {
				return proxyInterface.getAnnotation(annotationClass);
			}

			@Override
			public String getMethodSignature() {
				String parameters = Arrays.stream(method.getParameterTypes())
					.map(Class::getName)
					.collect(Collectors.joining(", "));

				return String.format("%s(%s)", method.getName(), parameters);
			}

			@Override
			public <T extends Annotation> T getMethodAnnotation(Class<T> annotationClass) {
				return method.getAnnotation(annotationClass);
			}

			@Override
			public Annotation[][] getMethodParameterAnnotations() {
				return method.getParameterAnnotations();
			}
		};

		boolean[] canCache = { true };

		SqlCompositeHelper sqlCompositeHelper;
		if (parameterBinderInfo.isPresent()) {
			var info = parameterBinderInfo.get();

			var names = new LinkedList<String>();
			var types = new LinkedList<AtomSqlType>();

			var size = info.fields.size();
			for (int i = 0; i < size; i++) {
				var field = info.fields.get(i);
				var value = info.values.get(i);

				var name = field.getName();

				names.add(name);

				var t = field.getType();
				if (t.equals(Object.class)) {
					// 型がObjectの場合、実際の値を見てPreparedStatementのsetメソッドを決定せざるを得ないので毎回判定が必要
					// そのためキャッシュさせない
					canCache[0] = false;

					if (value == null) {
						//値がnullの場合、仕方がないのでPreparedStatementにnullを設定できるようにNULLをセットする
						types.add(NULL.instance);
					} else {
						types.add(typeFactory.select(value.getClass()));
					}
				} else {
					types.add(typeFactory.select(field.getType()));
				}
			}

			var fieldNames = names.toArray(String[]::new);

			sqlCompositeHelper = sqlCompositeHelper(
				sql,
				confidentials,
				fieldNames,
				types.toArray(AtomSqlType[]::new),
				metadata.nonThreadSafe());
		} else {
			var types = Arrays.stream(metadata.parameterTypes()).map(c -> typeFactory.select(c)).toArray(AtomSqlType[]::new);

			sqlCompositeHelper = sqlCompositeHelper(
				sql,
				confidentials,
				parameterNames,
				types,
				metadata.nonThreadSafe());
		}

		//メソッドに付与されたアノテーション > クラスに付与されたアノテーション
		var nameAnnotation = qualifier(method).or(() -> qualifier(proxyInterface));

		SqlProxyHelper sqlProxyHelper = new SqlProxyHelper(
			nameAnnotation.map(a -> endpoints.get(a.value())).orElseGet(() -> endpoints.get()),
			metadata.result(),
			typeFactory,
			mySqlLogger,
			snapshot);

		return new HelpersResult(new Helpers(sqlProxyHelper, sqlCompositeHelper), canCache[0]);
	}

	private static record HelpersResult(Helpers helpers, boolean canCache) {}

	private static record Helpers(SqlProxyHelper sqlProxyHelper, SqlCompositeHelper sqlCompositeHelper) {}

	private static SqlCompositeHelper sqlCompositeHelper(
		SecureString secureSql,
		Set<String> confidentials,
		String[] parameterNames,
		AtomSqlType[] parameterTypes,
		boolean containsNonThreadSafeValue) {
		Map<String, AtomSqlType> map = new HashMap<>();
		for (int i = 0; i < parameterNames.length; i++) {
			map.put(parameterNames[i], parameterTypes[i]);
		}

		List<Component> components = new ArrayList<>();

		var sql = ColumnFinder.normalize(secureSql.toString());

		var sqlRemain = PlaceholderFinder.execute(sql, f -> {
			components.add(new Text(new SecureString(f.gap)));

			if (!map.containsKey(f.placeholder))
				throw new PlaceholderNotFoundException(f.placeholder);

			components.add(
				new Prototype(
					f.placeholder,
					confidentials.contains(f.placeholder),
					f.all,
					map.get(f.placeholder)));
		});

		components.add(new Text(new SecureString(sqlRemain)));

		return new SqlCompositeHelper(components, parameterNames, containsNonThreadSafeValue);
	}

	/**
	 * バッチ処理を実施します。<br>
	 * {@link Runnable}内で行われる更新処理はすべて、即時実行はされずに集められ、{@link Runnable}の処理が終了したのち一括で実行されます。<br>
	 * 大量の更新処理を行わなければならない場合、処理の高速化を見込むことが可能です。<br>
	 * 閾値に達した場合など一括実行中に例外が発生した場合、未実施の更新はすべて破棄されます。
	 * @param runnable 更新処理を含む汎用処理
	 */
	public void tryBatch(Runnable runnable) {
		var resources = new BatchResources();
		batchResources.set(resources);
		try {
			runnable.run();
		} finally {
			try {
				resources.flushAll();
			} finally {
				//バッチ実行中にエラーが発生した場合でも必ずクリア
				batchResources.remove();
			}
		}
	}

	/**
	 * バッチ処理を実施します。<br>
	 * {@link #tryBatch(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します<br>
	 * 閾値に達した場合など一括実行中に例外が発生した場合、未実施の更新はすべて破棄されます。
	 * @param <T> 返却値の型
	 * @see #tryBatch(Runnable)
	 * @param supplier 結果を返却が可能な更新処理を含む汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryBatch(Supplier<T> supplier) {
		var resources = new BatchResources();
		batchResources.set(resources);
		try {
			return supplier.get();
		} finally {
			try {
				resources.flushAll();
			} finally {
				//バッチ実行中にエラーが発生した場合でも必ずクリア
				batchResources.remove();
			}
		}
	}

	BatchResources batchResources() {
		return batchResources.get();
	}

	/**
	 * {@link Stream}を検索結果として使用する処理を実施します。<br>
	 * 処理内で発生した{@link Stream}は{@link Stream#close()}を明示的に行わなくても処理終了と同時にすべてクローズされます。
	 * @see Atom#stream()
	 * @see Atom#stream(RowMapper)
	 * @see Atom#stream(SimpleRowMapper)
	 * @param runnable {@link Stream}を使用した検索処理を含む汎用処理
	 */
	public void tryStream(Runnable runnable) {
		streams.set(new LinkedList<>());
		try {
			runnable.run();
		} finally {
			closeStreams();
			streams.remove();
		}
	}

	/**
	 * {@link Stream}を検索結果として使用する処理を実施します。<br>
	 * 処理内で発生した{@link Stream}は{@link Stream#close()}を明示的に行わなくても処理終了と同時にすべてクローズされます。
	 * {@link #tryStream(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します<br>
	 * @param <T> 返却値の型
	 * @see #tryStream(Runnable)
	 * @param supplier {@link Stream}を使用した検索処理を含む汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryStream(Supplier<T> supplier) {
		streams.set(new LinkedList<>());
		try {
			return supplier.get();
		} finally {
			closeStreams();
			streams.remove();
		}
	}

	private void closeStreams() {
		streams.get().forEach(s -> {
			try {
				s.close();
			} catch (Throwable t) {
				logger.log(Level.WARNING, "Error occured while closing a stream", t);
			}
		});
	}

	void registerStream(Stream<?> stream) {
		var list = streams.get();
		if (list == null) return;

		list.add(stream);
	}

	/**
	 * パラメーターに{@link NonThreadSafe}が付与されている型を使用する処理を実施します。<br>
	 * スレッドセーフではない値を使用した処理はすべてこの中で行われる必要があります。
	 * @see AtomSqlType
	 * @see NonThreadSafe
	 * @see NonThreadSafeException
	 * @param runnable パラメーターに{@link NonThreadSafe}が付与されている型を使用する汎用処理
	 */
	public void tryNonThreadSafe(Runnable runnable) {
		//既にtryNonThreadSafeの中で呼ばれた場合
		if (nonThreadSafeSqls.get() != null) {
			runnable.run();

			return;
		}

		nonThreadSafeSqls.set(new HashMap<>());
		try {
			runnable.run();
		} finally {
			nonThreadSafeSqls.remove();
		}
	}

	/**
	 * パラメーターに{@link NonThreadSafe}が付与されている型を使用する処理を実施します。<br>
	 * スレッドセーフではない値を使用した処理はすべてこの中で行われる必要があります。<br>
	 * {@link #tryNonThreadSafe(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します。
	 * @param <T> 返却値の型
	 * @see AtomSqlType
	 * @see NonThreadSafe
	 * @see NonThreadSafeException
	 * @param supplier パラメーターに{@link NonThreadSafe}が付与されている型を使用する汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryNonThreadSafe(Supplier<T> supplier) {
		//既にtryNonThreadSafeの中で呼ばれた場合
		if (nonThreadSafeSqls.get() != null) {
			return supplier.get();
		}

		nonThreadSafeSqls.set(new HashMap<>());
		try {
			return supplier.get();
		} finally {
			nonThreadSafeSqls.remove();
		}
	}

	void registerSqlCompositeForNonThreadSafe(Object key, SqlComposite sql) {
		sqlMap().put(key, sql);
	}

	SqlComposite getHelperForNonThreadSafe(Object key) {
		var result = sqlMap().get(key);
		if (result == null) throw new NonThreadSafeException();

		return result;
	}

	private Map<Object, SqlComposite> sqlMap() {
		var map = nonThreadSafeSqls.get();
		if (map == null) throw new NonThreadSafeException();

		return map;
	}

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。<br>
	 * 実装によっては、処理終了時に内部で使用する{@link Connection}がクローズされる可能性があります。<br>
	 * デフォルトであるプライマリ{@link Endpoint}が使用されます。<br>
	 * 処理内では、スレッドセーフではない値を使用することが可能です。
	 * @param consumer
	 */
	public void bollowConnection(Consumer<ConnectionProxy> consumer) {
		bollowConnection(null, consumer);
	}

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。<br>
	 * 実装によっては、処理終了時に内部で使用する{@link Connection}がクローズされる可能性があります。<br>
	 * qualifierによって使用する{@link Endpoint}を選択可能です。<br>
	 * 処理内では、スレッドセーフではない値を使用することが可能です。
	 * @param qualifier {@link Qualifier}に使用する値
	 * @param consumer
	 */
	public void bollowConnection(String qualifier, Consumer<ConnectionProxy> consumer) {
		tryNonThreadSafe(() -> endpoints.get(qualifier).endpoint().bollowConnection(consumer));
	}

	private static final String[] emptyStringArray = {};

	private static final AtomSqlType[] emptyAtomSqlTypeArray = {};

	private static final Object[] emptyObjectArray = {};

	SqlProxyHelper helper() {
		return new SqlProxyHelper(
			endpoints.get(),
			Object.class,
			typeFactory,
			sqlLogger,
			new SqlProxySnapshot() {

				@Override
				public String getMethodSignature() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Annotation[][] getMethodParameterAnnotations() {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T extends Annotation> T getMethodAnnotation(Class<T> annotationClass) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getClassName() {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T extends Annotation> T getClassAnnotation(Class<T> annotationClass) {
					throw new UnsupportedOperationException();
				}
			});
	}

	SqlComposite sqlComposite(SecureString sql) {
		return SqlComposite.createSqlComposite(
			sqlCompositeHelper(
				sql,
				null,
				emptyStringArray,
				emptyAtomSqlTypeArray,
				false),
			emptyObjectArray);
	}

	static record SqlProxyHelper(
		Endpoints.Entry entry,
		Class<?> resultClass,
		AtomSqlTypeFactory typeFactory,
		SqlLogger sqlLogger,
		SqlProxySnapshot snapshot) {

		static SqlProxyHelper newHelper(SqlProxyHelper base, Class<?> resultClass) {
			return new SqlProxyHelper(base.entry, resultClass, base.typeFactory, base.sqlLogger, base.snapshot);
		}
	}

	static void logElapsed(SqlLogger sqlLogger, long startNanos) {
		sqlLogger.logElapsed(logger -> {
			var elapsed = (System.nanoTime() - startNanos) / 1000000f;
			logger.log(Level.INFO, "elapsed: " + new BigDecimal(elapsed).setScale(2, RoundingMode.DOWN) + "ms");
		});
	}

	private static SecureString loadSql(Class<?> decreredClass, Method method) throws IOException {
		var proxyClassName = decreredClass.getName();

		var sqlContainer = method.getAnnotation(Sql.class);
		if (sqlContainer != null) {
			return new SecureString(sqlContainer.value());
		}

		var sqlFile = method.getAnnotation(SqlFile.class);
		if (sqlFile == null)
			throw new IllegalStateException("Method " + method.getName() + " requires " + Sql.class.getSimpleName() + " annotation or a " + SqlFile.class.getSimpleName() + " annotation");

		var sqlFileName = sqlFile.value();
		if (sqlFileName.isEmpty()) {
			sqlFileName = AtomSqlUtils.extractSimpleClassName(proxyClassName, decreredClass.getPackage().getName())
				+ "."
				+ method.getName()
				+ ".sql";
		}

		var url = decreredClass.getResource(sqlFileName);
		if (url == null)
			//sqlFileNameが見つかりませんでした
			throw new IllegalStateException(sqlFileName + " was not found");

		return new SecureString(new String(AtomSqlUtils.readBytes(url.openStream()), Constants.CHARSET));
	}
}
