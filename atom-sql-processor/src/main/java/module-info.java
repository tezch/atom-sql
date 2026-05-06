/**
 * Atom SQL Processor
 * @provides javax.annotation.processing.Processor
 * @author tezch
 */
module io.github.tezch.atomsql.processor {

	requires java.compiler;

	requires io.github.tezch.atomsql.core;

	provides javax.annotation.processing.Processor
		with io.github.tezch.atomsql.processor.AtomSqlProcessor;
}
