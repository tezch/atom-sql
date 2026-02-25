package io.github.tezch.atomsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class SqlComposite {

	static final SqlComposite EMPTY = new SqlComposite(new SecureString(""));

	static final SqlComposite BLANK = new SqlComposite(new SecureString(" "));

	static final SqlComposite AND = new SqlComposite(new SecureString(" AND "));

	static final SqlComposite OR = new SqlComposite(new SecureString(" OR "));

	static record SqlCompositeHelper(
		List<Component> prototypes,
		String[] parameterNames,
		boolean containsNonThreadSafeValue) {}

	static record Compiled(List<Placeholder> placeholders, SecureString sql) {

		String sqlString() {
			return sql.toString();
		}
	}

	static SqlComposite createSqlComposite(SqlCompositeHelper helper, Object[] args) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < helper.parameterNames.length; i++) {
			map.put(helper.parameterNames[i], args[i]);
		}

		List<Component> components = helper.prototypes.stream().map(p -> p.complement(map)).toList();

		return new SqlComposite(components, helper.containsNonThreadSafeValue);
	}

	static interface Component {

		void replaceAndAdd(Pattern pattern, SqlComposite another, List<Component> components);

		void placeholder(Consumer<Placeholder> consumer);

		Component complement(Map<String, Object> map);

		void appendTo(StringBuilder builder);

		void appendOriginalTo(StringBuilder builder);

		boolean isEmpty();

		boolean isBlank();
	}

	static record Text(SecureString text) implements Component {

		@Override
		public void replaceAndAdd(Pattern pattern, SqlComposite another, List<Component> components) {
			String remain = text.toString();
			while (true) {
				var matcher = pattern.matcher(remain);

				if (!matcher.find())
					break;

				components.add(new Text(new SecureString(remain.substring(0, matcher.start()))));

				remain = remain.substring(matcher.end());

				components.addAll(another.components);
			}

			components.add(new Text(new SecureString(remain)));
		}

		@Override
		public void placeholder(Consumer<Placeholder> consumer) {}

		@Override
		public Component complement(Map<String, Object> map) {
			return this;
		}

		@Override
		public void appendTo(StringBuilder builder) {
			builder.append(text);
		}

		@Override
		public void appendOriginalTo(StringBuilder builder) {
			builder.append(text);
		}

		@Override
		public boolean isEmpty() {
			return text.toString().isEmpty();
		}

		@Override
		public boolean isBlank() {
			return text.toString().isBlank();
		}
	}

	static record Placeholder(
		String name, //プレースホルダ名
		boolean confidential,
		SecureString expression, //置換後のプレースホルダ
		String original, //元のプレースホルダ文字列全体（型ヒントを含む）
		AtomSqlType type, //型
		Object value //値
	) implements Component {

		@Override
		public void replaceAndAdd(Pattern pattern, SqlComposite another, List<Component> components) {
			components.add(this);
		}

		@Override
		public void placeholder(Consumer<Placeholder> consumer) {
			consumer.accept(this);
		}

		@Override
		public Component complement(Map<String, Object> map) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void appendTo(StringBuilder builder) {
			builder.append(expression);
		}

		@Override
		public void appendOriginalTo(StringBuilder builder) {
			builder.append(original);
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean isBlank() {
			return false;
		}
	}

	static record Prototype(
		String name, //プレースホルダ名
		boolean confidential,
		String original, //元のプレースホルダ文字列全体（型ヒントを含む）
		AtomSqlType type //型
	) implements Component {

		@Override
		public void replaceAndAdd(Pattern pattern, SqlComposite another, List<Component> components) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void placeholder(Consumer<Placeholder> consumer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Component complement(Map<String, Object> map) {
			var value = map.get(name);
			return new Placeholder(
				name,
				confidential,
				new SecureString(type.placeholderExpression(value)),
				original,
				type,
				value);
		}

		@Override
		public void appendTo(StringBuilder builder) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void appendOriginalTo(StringBuilder builder) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isBlank() {
			throw new UnsupportedOperationException();
		}
	}

	final boolean containsNonThreadSafeValue;

	private final List<Component> components;

	private final Compiled compiled;

	private SqlComposite(List<Component> components, boolean containsNonThreadSafeValue) {
		this.components = List.copyOf(new ArrayList<>(components));
		this.containsNonThreadSafeValue = containsNonThreadSafeValue;
		compiled = new Compiled(List.copyOf(placeholders()), new SecureString(string()));
	}

	private SqlComposite(SecureString text) {
		this.components = Collections.singletonList(new Text(text));
		containsNonThreadSafeValue = false;
		compiled = new Compiled(placeholders(), new SecureString(string()));
	}

	Compiled compiled() {
		return compiled;
	}

	List<Placeholder> placeholders() {
		List<Placeholder> placeholders = new ArrayList<>();
		components.forEach(e -> e.placeholder(placeholders::add));

		return placeholders;
	}

	private String string() {
		var builder = new StringBuilder();
		components.forEach(e -> e.appendTo(builder));

		return builder.toString();
	}

	String originalString() {
		var builder = new StringBuilder();
		components.forEach(e -> e.appendOriginalTo(builder));

		return builder.toString();
	}

	SqlComposite replace(Pattern pattern, SqlComposite another) {
		List<Component> components = new ArrayList<>();
		this.components.forEach(e -> e.replaceAndAdd(pattern, another, components));

		return new SqlComposite(components, containsNonThreadSafeValue || another.containsNonThreadSafeValue);
	}

	SqlComposite concat(SqlComposite another) {
		List<Component> components = new ArrayList<>();
		components.addAll(this.components);
		components.addAll(another.components);

		return new SqlComposite(components, containsNonThreadSafeValue || another.containsNonThreadSafeValue);
	}

	SqlComposite join(SecureString prefix, SecureString suffix) {
		List<Component> components = new ArrayList<>();
		components.add(new Text(prefix));
		components.addAll(this.components);
		components.add(new Text(suffix));

		return new SqlComposite(components, containsNonThreadSafeValue);
	}

	boolean isEmpty() {
		if (components.size() == 0) return true;

		return components.stream().allMatch(Component::isEmpty);
	}

	boolean isBlank() {
		if (components.size() == 0) return true;

		return components.stream().allMatch(Component::isBlank);
	}

	@Override
	public String toString() {
		return string();
	}
}
