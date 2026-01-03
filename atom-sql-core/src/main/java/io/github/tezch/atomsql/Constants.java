package io.github.tezch.atomsql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.github.tezch.atomsql.annotation.ConfidentialSql;
import io.github.tezch.atomsql.annotation.DataObject;
import io.github.tezch.atomsql.annotation.SqlProxy;

/**
 * Atom SQL内部で使用される定数を保持するクラスです。
 * @author tezch
 */
public interface Constants {

	/**
	 * {@link SqlProxy}メタ情報保持クラスの名称サフィックス
	 */
	public static final String METADATA_CLASS_SUFFIX = "$AtomSqlMetadata";

	/**
	 * {@link DataObject}メタ情報保持クラスの名称サフィックス
	 */
	public static final String DATA_OBJECT_METADATA_CLASS_SUFFIX = "$AtomSqlDataObjectMetadata";

	/**
	 * SQLファイル、その他Atom SQLで使用する入出力ファイルの文字コード
	 */
	public static final Charset CHARSET = StandardCharsets.UTF_8;

	/**
	 * 改行コード
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * {@link SqlProxy}メタ情報保持クラスの一覧ファイル名
	 */
	public static final String PROXY_LIST = "io.github.tezch.atom-sql.proxy-list";

	/**
	 * {@link ConfidentialSql}が付与されたSQL文のログ上の目印
	 */
	public static final String CONFIDENTIAL = "<<CONFIDENTIAL>>";
}
