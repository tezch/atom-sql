package io.github.tezch.atomsql;

import java.lang.annotation.Annotation;

import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * 実行中の{@link SqlProxy}とそのメソッドの情報を取得するためのクラスです。<br>
 * {@link SqlProxy}のメソッドを実行中ではない場合、このクラスのインスタンスからは値を取得することができません。
 * @author tezch
 */
public interface SqlProxySnapshot {

	/**
	 * 実行中の{@link SqlProxy}のクラス名を返します。
	 * @return {@link SqlProxy}のクラス名
	 */
	public String getClassName();

	/**
	 * 実行中の{@link SqlProxy}のメソッドシグネチャを返します。
	 * @return 実行中の{@link SqlProxy}のメソッドシグネチャ
	 */
	public String getMethodSignature();

	/**
	 * 実行中の{@link SqlProxy}に付与されているアノテーションを返します。
	 * @param <T> アノテーション
	 * @param annotationClass アノテーションのクラス
	 * @return 実行中の{@link SqlProxy}に付与されているアノテーション
	 */
	public <T extends Annotation> T getClassAnnotation(Class<T> annotationClass);

	/**
	 * 実行中の{@link SqlProxy}のメソッドに付与されているアノテーションを返します。
	 * @param <T> アノテーション
	 * @param annotationClass アノテーションのクラス
	 * @return 実行中の{@link SqlProxy}のメソッドに付与されているアノテーション
	 */
	public <T extends Annotation> T getMethodAnnotation(Class<T> annotationClass);

	/**
	 * 実行中の{@link SqlProxy}のメソッドのパラメータに付与されているアノテーションを返します。
	 * @return 実行中の{@link SqlProxy}のメソッドのパラメータに付与されているアノテーション
	 */
	public Annotation[][] getMethodParameterAnnotations();
}
