/**
 * Atom SQL Spring
 * @author tezch
 */
module io.github.tezch.atomsql.spring {

	requires transitive io.github.tezch.atomsql.core;

	requires spring.beans;

	requires transitive spring.context;

	requires transitive spring.jdbc;

	requires spring.core;

	requires spring.tx;

	exports io.github.tezch.atomsql.spring;
}
