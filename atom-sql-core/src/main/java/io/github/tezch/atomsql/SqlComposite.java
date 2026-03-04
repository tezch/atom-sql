package io.github.tezch.atomsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import io.github.tezch.atomsql.type.CSV;
import io.github.tezch.atomsql.type.NULL;
import io.github.tezch.atomsql.type.OBJECT;

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

	static SqlComposite createSqlComposite(
		SqlCompositeHelper helper,
		Object[] args,
		AtomSqlTypeFactory typeFactory) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < helper.parameterNames.length; i++) {
			map.put(helper.parameterNames[i], args[i]);
		}

		List<Component> components = helper.prototypes.stream().map(p -> p.bind(map, typeFactory)).toList();

		boolean containsNonThreadSafeValue = helper.containsNonThreadSafeValue;
		if (containsNonThreadSafeValue) {
			//processor処理時にnonThreadSafeと判定されている場合、今回の値を使って再度nonThreadSafe確認
			containsNonThreadSafeValue = components.stream()
				.filter(c -> c.nonThreadSafeValue(typeFactory))
				.findFirst()
				.isPresent();
		}

		return new SqlComposite(components, containsNonThreadSafeValue);
	}

	static SqlComposite rebind(SqlComposite base, Map<String, Object> values, AtomSqlTypeFactory typeFactory) {
		List<Component> components = base.components.stream().map(p -> p.bind(values, typeFactory)).toList();

		return new SqlComposite(
			components,
			//値を取り込みなおしてnonThreadSafe確認
			components.stream().filter(c -> c.nonThreadSafeValue(typeFactory)).findFirst().isPresent());
	}

	static interface Component {

		void replaceAndAdd(Pattern pattern, SqlComposite another, List<Component> components);

		void placeholder(Consumer<Placeholder> consumer);

		Component bind(Map<String, Object> values, AtomSqlTypeFactory typeFactory);

		void appendTo(StringBuilder builder);

		void appendOriginalTo(StringBuilder builder);

		boolean nonThreadSafeValue(AtomSqlTypeFactory typeFactory);

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
		public Component bind(Map<String, Object> values, AtomSqlTypeFactory typeFactory) {
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
		public boolean nonThreadSafeValue(AtomSqlTypeFactory typeFactory) {
			return false;
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
		AtomSqlType type, //Object型の場合、実際の値から判定した型
		AtomSqlType staticType, //元々指定された型
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
		public Component bind(Map<String, Object> values, AtomSqlTypeFactory typeFactory) {
			//キーが存在しない場合、値の更新はないものとして自身を返す
			if (!values.containsKey(name)) {
				return this;
			}

			var value = values.get(name);

			var computedType = computeType(staticType, value, typeFactory);

			return new Placeholder(
				name,
				confidential,
				new SecureString(computedType.placeholderExpression(value)),
				original,
				computedType,
				staticType,
				value);
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
		public boolean nonThreadSafeValue(AtomSqlTypeFactory typeFactory) {
			return CSV.tryNonThreadSafe(value, typeFactory, () -> type.nonThreadSafe());
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
		public Component bind(Map<String, Object> values, AtomSqlTypeFactory typeFactory) {
			var value = values.get(name);

			var computedType = computeType(type, value, typeFactory);

			return new Placeholder(
				name,
				confidential,
				new SecureString(computedType.placeholderExpression(value)),
				original,
				computedType,
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
		public boolean nonThreadSafeValue(AtomSqlTypeFactory typeFactory) {
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

	private static AtomSqlType computeType(AtomSqlType type, Object value, AtomSqlTypeFactory typeFactory) {
		if (type != OBJECT.instance) return type;

		//型がOBJECTの場合
		//値がnullの場合、仕方がないのでPreparedStatementにnullを設定できるようにNULLをセットする
		//nullでなければ値から型を判定
		return value == null ? NULL.instance : typeFactory.select(value.getClass());
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
