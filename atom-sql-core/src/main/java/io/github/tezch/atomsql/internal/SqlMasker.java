package io.github.tezch.atomsql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.tezch.atomsql.AtomSql;

/**
 * SQL解析の邪魔になるリテラルやコメントを隠し、復元します。
 * @author tezch
 */
public class SqlMasker {

	private static final String boundary = "[" + AtomSql.class.getSimpleName() + "#" + UUID.randomUUID() + "]";

	private static final Pattern tagPattern;

	static {
		var escapedBoundary = Pattern.quote(boundary);
		tagPattern = Pattern.compile(escapedBoundary + "(\\d+)" + escapedBoundary);
	}

	private static final Pattern pattern = Pattern.compile(
		/*
		 * <string> 文字列 複数行にまたがりマッチ
		 * <id> 識別子 複数行にまたがりマッチ
		 * <comment> コメント 行末までマッチ .はPattern.DOTALLを使用しなければ改行コードにはヒットしないので行末までという意味になる
		 */
		"(?<string>'(''|[^'])*')|(?<id>\"(\"\"|[^\"])*\")|(?<comment>--.*|//.*)");

	private final List<String> stash = new ArrayList<>();

	/**
	 * SQLからリテラルやコメントを隠します。
	 * @param sql SQL
	 * @return マスクされたSQL
	 */
	public String mask(String sql) {
		Objects.requireNonNull(sql);

		var masked = new StringBuilder();

		var matcher = pattern.matcher(sql);
		while (matcher.find()) {
			var found = matcher.group();

			String replacement;

			if (matcher.group("id") != null && isJavaIdentifier(found)) {
				//idとしてヒットしたがJava識別子の場合(自動生成DataObjectカラム候補)はマスクスキップ
				replacement = found;
			} else {
				stash.add(matcher.group());
				// 復元用のタグを生成 stashの現在のインデックスを使用
				replacement = boundary + (stash.size() - 1) + boundary;
			}

			matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
		}

		matcher.appendTail(masked);

		return masked.toString();
	}

	/**
	 * このインスタンスでマスクしたSQLを元に戻します。
	 * @param maskedSql masked
	 * @return unmasked
	 */
	public String unmask(String maskedSql) {
		Objects.requireNonNull(maskedSql);

		var unmasked = new StringBuilder();

		var matcher = tagPattern.matcher(maskedSql);

		while (matcher.find()) {
			int index = Integer.parseInt(matcher.group(1));

			// stashから元の文字列を取得して戻す（quoteReplacementで特殊文字を保護）
			matcher.appendReplacement(unmasked, Matcher.quoteReplacement(stash.get(index)));
		}

		matcher.appendTail(unmasked);

		return unmasked.toString();
	}

	private static boolean isJavaIdentifier(String s) {
		return AtomSqlUtils.isSafeJavaIdentifier(s.substring(1, s.length() - 1));
	}
}
