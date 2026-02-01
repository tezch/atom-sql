package io.github.tezch.atomsql;

import java.util.Set;

import io.github.tezch.atomsql.InnerSql.Text;

final class SecureString {

	private static Set<Class<?>> permittedCallers = Set.of(
		Text.class,
		InnerSql.class,
		SqlProxyHelper.class,
		Atom.class,
		AtomSql.class);

	private final String value;

	SecureString(String value) {
		var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();

		if (!permittedCallers.contains(caller)) {
			throw new SecurityException("Direct access not allowed");
		}

		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}
}
