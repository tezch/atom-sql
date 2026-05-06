/**
 * Atom SQL
 * @author tezch
 */
module io.github.tezch.atomsql.core {

	requires transitive java.sql;

	requires transitive java.compiler;

	exports io.github.tezch.atomsql;

	exports io.github.tezch.atomsql.annotation;

	exports io.github.tezch.atomsql.annotation.processor;

	exports io.github.tezch.atomsql.type;

	exports io.github.tezch.atomsql.internal to
		io.github.tezch.atomsql.processor,
		io.github.tezch.atomsql.spring;
}
