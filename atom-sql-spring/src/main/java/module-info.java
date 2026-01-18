/**
 * Atom SQL Spring
 * @author tezch
 */
module io.github.tezch.atomsql.spring {

	requires io.github.tezch.atomsql.core;

	requires spring.beans;

	requires spring.context;

	requires spring.jdbc;

	requires spring.core;

	requires spring.tx;

	exports io.github.tezch.atomsql.spring;
}
