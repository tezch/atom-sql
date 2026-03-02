/**
 * Atom SQL Spring Boot Starter
 * @author tezch
 */
module io.github.tezch.atomsql.spring.boot {

	requires spring.core;

	requires spring.beans;

	requires spring.context;

	requires spring.jdbc;

	requires spring.boot;

	requires spring.boot.autoconfigure;

	requires spring.boot.jdbc;

	requires io.github.tezch.atomsql.spring;
}
