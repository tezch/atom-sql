package io.github.tezch.atomsql.processor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import io.github.tezch.atomsql.Constants;
import io.github.tezch.atomsql.PlaceholderFinder;
import io.github.tezch.atomsql.type.OBJECT;

class ParameterBinderBuilder extends HelperBuilder {

	ParameterBinderBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
	}

	@Override
	ExtractResult extractTargetElement(ExecutableElement method) {
		var parameters = method.getParameters();

		if (parameters.size() != 1 || !ProcessorUtils.sameClass(toTypeElement(parameters.get(0)), Consumer.class)) {
			//メソッドeは、Consumerの1つのパラメータを必要とします
			error(
				"Method ["
					+ method.getSimpleName()
					+ "] requires one parameter of Consumer",
				method);

			return ExtractResult.fail;
		}

		var p = parameters.get(0);

		var typeArgs = ProcessorUtils.getTypeArgument(p);
		if (typeArgs.size() == 0) {
			//Consumerと書かれた場合
			//Consumerは、型の引数を必要とします
			error("Consumer requires a type argument", p);

			return ExtractResult.fail;
		}

		var typeArg = typeArgs.get(0);

		var element = ProcessorUtils.toElement(typeArg);
		if (element == null) {
			//Consumer<?>と書かれた場合
			//Consumerは、型の引数を必要とします
			error("Consumer requires a type argument", p);

			return ExtractResult.fail;
		}

		return new ExtractResult(true, element);
	}

	@Override
	Class<?> template() {
		return ParameterBinder_Template.class;
	}

	@Override
	void processFields(ExecutableElement method, String sql, Map<String, String> param) {
		var dubplicateChecker = new HashSet<String>();
		var fields = new LinkedList<String>();
		var enumValidators = new LinkedList<String>();
		PlaceholderFinder.execute(sql, f -> {
			//重複は除外
			if (dubplicateChecker.contains(f.placeholder)) return;

			dubplicateChecker.add(f.placeholder);

			var typeFactory = ProcessorTypeFactory.instance;

			var typeArgument = f.typeArgumentHint
				.map(typeFactory::typeArgumentOf)
				.map(t -> "<" + t.typeArgumentExpression() + ">")
				.orElse("");

			var type = f.typeHint.map(typeFactory::typeOf).orElse(OBJECT.instance);

			ProcessorUtils.enumValidator(type, f.placeholder).ifPresent(enumValidators::add);

			ProcessorUtils.enumValidator(
				f.typeArgumentHint
					.map(typeFactory::typeOf)
					.orElse(OBJECT.instance),
				f.placeholder).ifPresent(enumValidators::add);

			var field = "public "
				+ type.typeExpression()
				+ typeArgument
				+ " "
				+ f.placeholder
				+ ";";
			fields.add(field);
		});

		param.put("FIELDS", String.join(Constants.NEW_LINE, fields));

		param.put("ENUM_VALIDATORS", String.join(Constants.NEW_LINE, enumValidators));
	}

	private static TypeElement toTypeElement(VariableElement e) {
		return ProcessorUtils.toTypeElement(ProcessorUtils.toElement(e.asType()));
	}
}
