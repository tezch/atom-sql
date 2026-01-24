package io.github.tezch.atomsql.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

import io.github.tezch.atomsql.Atom;
import io.github.tezch.atomsql.ColumnFinder;
import io.github.tezch.atomsql.Constants;
import io.github.tezch.atomsql.Protoatom;
import io.github.tezch.atomsql.annotation.DataObject;
import io.github.tezch.atomsql.processor.MethodExtractor.Result;
import io.github.tezch.atomsql.type.OBJECT;

/**
 * @author tezch
 */
class DataObjectBuilder extends SourceBuilder {

	DataObjectBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
	}

	@Override
	ExtractResult extractTargetElement(ExecutableElement method) {
		var returnType = method.getReturnType();

		var typeElement = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(returnType));

		var requires = List.of(List.class, Stream.class, Optional.class, Atom.class, Protoatom.class);

		var found = requires.stream().filter(c -> ProcessorUtils.sameClass(typeElement, c)).findFirst();

		if (found.isEmpty()) {
			//メソッドeは、返す型としてrequiresを必要とします
			error(
				"Method ["
					+ method.getSimpleName()
					+ "] requires returning "
					+ String.join(" or ", requires.stream().map(c -> c.getSimpleName()).toList()),
				method);

			return ExtractResult.fail;
		}

		var args = ProcessorUtils.getTypeArgument(returnType);

		if (args.isEmpty()) {
			error(Protoatom.class.getSimpleName() + " requires type argument(s)", method);

			return ExtractResult.fail;
		}

		var element = ProcessorUtils.toElement(args.get(0));
		if (element == null) {
			//?とされた場合
			error(found.get().getSimpleName() + " requires " + DataObject.class.getSimpleName() + " type arguments", method);

			return ExtractResult.fail;
		}

		return new ExtractResult(true, element);
	}

	private static record Column(String typeExpression, String column) {}

	private void columns(String sql, List<Column> columns, List<String> enumValidators, ExecutableElement method) {
		var dubplicateChecker = new HashSet<String>();

		ColumnFinder.execute(sql, f -> {
			var column = f.column;
			//重複は除外
			if (dubplicateChecker.contains(column)) return;

			dubplicateChecker.add(column);

			var typeFactory = ProcessorTypeFactory.instance;

			String typeExpression;

			if (!"OPT".equals(f.typeHint)) {
				if (f.typeArgumentHint.isPresent()) {
					//DataObjectの型ヒントにf.typeHintは使用できません
					error(f.typeHint + " is not allowed as a type hint for " + DataObject.class.getSimpleName(), method);

					return;
				}

				var type = typeFactory.typeOf(f.typeHint);

				typeExpression = type.typeExpression();

				ProcessorUtils.enumValidator(type, column).ifPresent(enumValidators::add);
			} else {
				typeExpression = Optional.class.getName()
					+ f.typeArgumentHint
						.map(typeFactory::typeArgumentOf)
						.map(t -> "<" + t.typeArgumentExpression() + ">")
						.get();

				ProcessorUtils.enumValidator(
					f.typeArgumentHint
						.map(typeFactory::typeOf)
						.orElse(OBJECT.instance),
					column).ifPresent(enumValidators::add);
			}

			columns.add(new Column(typeExpression, column));
		});
	}

	@Override
	String source(String generateClassName, ExecutableElement method, Result result) {
		var columns = new LinkedList<Column>();

		var enumValidators = new LinkedList<String>();

		columns(result.sql, columns, enumValidators, method);

		if (columns.size() > 100) {
			return tooManyColumnsDataObject(generateClassName, result, columns, enumValidators);
		}

		return dataObject(generateClassName, result, columns, enumValidators);
	}

	private String dataObject(
		String generateClassName,
		Result result,
		List<Column> columns,
		LinkedList<String> enumValidators) {
		var template = Formatter.readTemplate(DataObject_Template.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		param.put(
			"COLUMNS",
			String.join(
				"," + Constants.NEW_LINE,
				columns.stream().map(c -> c.typeExpression + " " + c.column).toList()));

		param.put("ENUM_VALIDATORS", String.join(Constants.NEW_LINE, enumValidators));

		return Formatter.format(template, param);
	}

	private String tooManyColumnsDataObject(
		String generateClassName,
		Result result,
		List<Column> columns,
		LinkedList<String> enumValidators) {
		var template = Formatter.readTemplate(TooManyColumnsDataObject_Template.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		param.put(
			"COLUMNS",
			String.join(
				Constants.NEW_LINE,
				columns.stream().map(c -> String.format("public %s %s;", c.typeExpression, c.column)).toList()));

		param.put(
			"FIELDS",
			String.join(
				Constants.NEW_LINE,
				columns.stream().map(c -> String.format("private final %s %s;", c.typeExpression, c.column)).toList()));

		param.put(
			"METHODS",
			String.join(
				Constants.NEW_LINE,
				columns.stream().map(c -> String.format("public %s %s() {return %s;}", c.typeExpression, c.column, c.column)).toList()));

		param.put(
			"BEAN_TO_COLUMNS",
			String.join(
				Constants.NEW_LINE,
				columns.stream().map(c -> String.format("%s = bean.%s;", c.column, c.column)).toList()));

		param.put("ENUM_VALIDATORS", String.join(Constants.NEW_LINE, enumValidators));

		return Formatter.format(template, param);
	}
}
