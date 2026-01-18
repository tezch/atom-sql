package io.github.tezch.atomsql;

import java.util.Optional;

/**
 * @author tezch
 */
public interface AtomSqlTypeFactory {

	/**
	* 渡されたクラスから対応する{@link AtomSqlType}を返します。
	* @param c 対象となるクラス
	* @return {@link AtomSqlType}
	* @throws UnknownSqlTypeException {@link AtomSqlType}に定義されていない型を使用した場合
	*/
	AtomSqlType select(Class<?> c);

	/**
	 * タイプ名をもとに{@link AtomSqlType}のインスタンスを返します。
	 * @param name {@link AtomSqlType}タイプ名
	 * @return {@link AtomSqlType}
	 */
	Optional<AtomSqlType> typeOf(String name);

	/**
	 * プリミティブ型ではない型のうち、使用可能な型かどうかを返します。<br>
	 * {@link Csv}は、パラメータとして使用可能ですが、ここでは除外されます。
	 * @param c {@link Class}
	 * @return 使用可能な型の場合、true
	 */
	boolean canUse(Class<?> c);

	/**
	 * プリミティブ型ではない型のうち、使用可能な型を返します。
	 * @return 使用可能な型の配列
	 */
	AtomSqlType[] nonPrimitiveTypes();

	/**
	 * クラス名からインスタンスを生成します。
	 * @param className {@link AtomSqlTypeFactory}を実装したクラス名
	 * @param loader {@link ClassLoader}
	 * @return {@link AtomSqlTypeFactory}
	 */
	public static AtomSqlTypeFactory newInstance(String className, ClassLoader loader) {
		if (className == null || className.isBlank() || className.equals(DefaultAtomSqlTypeFactory.class.getName()))
			return DefaultAtomSqlTypeFactory.instance;

		try {
			return (AtomSqlTypeFactory) Class.forName(className, true, loader).getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
