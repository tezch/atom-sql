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

	private void columns(String sql, List<String> columns, List<String> enumValidators) {
		var dubplicateChecker = new HashSet<String>();

		ColumnFinder.execute(sql, f -> {
			var column = f.column;
			//重複は除外
			if (dubplicateChecker.contains(column)) return;

			dubplicateChecker.add(column);

			var type = f.typeHint.map(ProcessorTypeFactory.instance::typeOf).orElse(OBJECT.instance);

			columns.add(type.typeExpression() + " " + column);

			ProcessorUtils.enumValidator(type, column).ifPresent(enumValidators::add);
		});
	}

	@Override
	String source(String generateClassName, ExecutableElement method, Result result) {
		var template = Formatter.readTemplate(DataObject_Template.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		var columns = new LinkedList<String>();

		var enumValidators = new LinkedList<String>();

		columns(result.sql, columns, enumValidators);

		param.put("COLUMNS", String.join("," + Constants.NEW_LINE, columns));

		param.put("ENUM_VALIDATORS", String.join(Constants.NEW_LINE, enumValidators));

		return Formatter.format(template, param);
	}
}
