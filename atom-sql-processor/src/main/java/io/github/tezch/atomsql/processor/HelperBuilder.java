package io.github.tezch.atomsql.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

import io.github.tezch.atomsql.processor.MethodExtractor.Result;

/**
 * @author tezch
 */
abstract class HelperBuilder extends SourceBuilder {

	HelperBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
	}

	abstract void processFields(ExecutableElement method, String sql, Map<String, String> param);

	abstract Class<?> template();

	@Override
	String source(String generateClassName, ExecutableElement method, Result result) {
		var template = Formatter.readTemplate(template(), "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		processFields(method, result.sql, param);

		return Formatter.format(template, param);
	}
}
