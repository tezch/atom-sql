package io.github.tezch.atomsql;

import io.github.tezch.atomsql.internal.PropertiesConfiguration;

/**
 * 内部使用クラス
 */
public class AtomSqlInitializer {

	private static final ThreadLocal<Configuration> configHolder = ThreadLocal.withInitial(() -> configurationInternal());

	private static Configuration staticConfig;

	/**
	 * 
	 * @param config
	 */
	public synchronized static void initialize(Configuration config) {
		if (staticConfig != null) throw new IllegalStateException("Atom SQL is already initialized");
		staticConfig = config;
	}

	/**
	 * initialize
	 */
	public static void initialize() {
		initialize(new PropertiesConfiguration());
	}

	/**
	 * initializeIfUninitialized
	 * @param config 
	 */
	public synchronized static void initializeIfUninitialized(Configuration config) {
		if (staticConfig != null) return;
		staticConfig = config;
	}

	/**
	 * initializeIfUninitialized
	 */
	public synchronized static void initializeIfUninitialized() {
		if (staticConfig != null) return;
		initialize(new PropertiesConfiguration());
	}

	/**
	 * @return {@link Configuration}
	 */
	public static Configuration configuration() {
		return configHolder.get();
	}

	private synchronized static Configuration configurationInternal() {
		if (staticConfig == null) throw new IllegalStateException("Atom SQL is not initialized");
		return staticConfig;
	}
}
