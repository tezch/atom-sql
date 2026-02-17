package io.github.tezch.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import io.github.tezch.atomsql.AtomSqlUtils;
import io.github.tezch.atomsql.Constants;
import io.github.tezch.atomsql.annotation.SqlProxy;
import io.github.tezch.atomsql.processor.MethodExtractor.Result;
import io.github.tezch.atomsql.processor.MethodExtractor.SqlNotFoundException;
import io.github.tezch.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

/**
 * @author tezch
 */
abstract class SourceBuilder {

	private final Supplier<ProcessingEnvironment> processingEnv;

	private final DuplicateClassChecker checker;

	private final MethodExtractor extractor;

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	SourceBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		this.processingEnv = processingEnv;
		this.checker = checker;
		extractor = new MethodExtractor(processingEnv);
	}

	static class DuplicateClassChecker {

		/**
		 * 生成したクラスの一覧ファイル名
		 */
		private static final String fileName = "io.github.tezch.atom-sql.helper-list";

		//生成クラス名, メソッド名
		private final Map<String, MethodInfo> allHelperNames = new HashMap<>();

		void start(ProcessingEnvironment env) {
			try {
				//他のクラスで作られた過去分を追加
				if (Files.exists(ProcessorUtils.getClassOutputPath(env).resolve(fileName))) {
					var listFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
					try (var input = listFile.openInputStream()) {
						Arrays.stream(new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
							.filter(l -> l.length() > 0)//空の場合スキップ
							.map(l -> new MethodInfo(l))
							.forEach(i -> allHelperNames.put(i.generatedClass, i));
					}
				}
			} catch (IOException e) {
				env.getMessager().printMessage(Kind.ERROR, e.getMessage());
				return;
			}
		}

		void finish(ProcessingEnvironment env) {
			var data = String.join(Constants.NEW_LINE, (allHelperNames.values().stream().map(i -> i.pack()).toList()));

			try (var output = new BufferedOutputStream(
				env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName).openOutputStream())) {
				output.write(data.getBytes(Constants.CHARSET));
			} catch (IOException e) {
				env.getMessager().printMessage(Kind.ERROR, e.getMessage());
			}
		}

		/**
		 * これから処理する{@link SqlProxy}分は削除しておき、新たに全生成クラスを検査可能にする
		 */
		void startSqlProxy(String binaryClassName) {
			allHelperNames.entrySet().removeIf(e -> e.getValue().enclosingClass.equals(binaryClassName));
		}

		private void put(String className, MethodInfo info) {
			allHelperNames.put(className, info);
		}

		private MethodInfo get(String className) {
			return allHelperNames.get(className);
		}
	}

	void execute(ExecutableElement method) {
		var result = extractTargetElement(method);

		if (!result.success) return;

		execute(method, result.targetType);
	}

	abstract ExtractResult extractTargetElement(ExecutableElement method);

	static record ExtractResult(boolean success, Element targetType) {

		static final ExtractResult fail = new ExtractResult(false, null);
	}

	abstract String source(String generateClassName, ExecutableElement method, Result result);

	private void execute(ExecutableElement method, Element targetType) {
		String generatePackageName;
		String generateClassName;
		{
			var typeElement = ProcessorUtils.toTypeElement(targetType);
			var className = typeElement.getQualifiedName().toString();

			generatePackageName = ProcessorUtils.getPackageName(typeElement);

			generateClassName = AtomSqlUtils.extractSimpleClassName(className, generatePackageName);
		}

		Result result;
		try {
			result = extractor.execute(method);
		} catch (SqlNotFoundException | SqlFileNotFoundException ex) {
			error(ex.getMessage(), method);
			return;
		}

		var className = result.className;

		//パッケージはSqlProxyのあるパッケージ固定
		var packageName = result.packageName;

		var methodSignature = methodSignature(method);

		var newClassName = packageName.isEmpty() ? generateClassName : packageName + "." + generateClassName;

		var info = checker.get(newClassName);
		if (info != null && (!info.enclosingClass.equals(className) || !info.method.equals(methodSignature))) {
			//generateClassNameという名前は既に他で使われています
			error("The name [" + generateClassName + "] has already been used elsewhere", method);
			return;
		}

		checker.put(newClassName, new MethodInfo(newClassName, className, methodSignature));

		if (alreadyCreatedFiles.contains(newClassName)) return;

		try {
			try (var output = new BufferedOutputStream(processingEnv.get().getFiler().createSourceFile(newClassName, method).openOutputStream())) {
				output.write(source(generateClassName, method, result).getBytes(Constants.CHARSET));
			}

			alreadyCreatedFiles.add(newClassName);
		} catch (IOException ioe) {
			error(ioe.getMessage(), method);
		}
	}

	private static String methodSignature(ExecutableElement method) {
		String parameters = method.getParameters()
			.stream()
			.map(VariableElement::asType)
			.map(SourceBuilder::toString)
			.collect(Collectors.joining(", "));

		return String.format("%s(%s)", method.getSimpleName().toString(), parameters);
	}

	private static String toString(TypeMirror type) {
		// プリミティブ型はElementに変換できないため直接文字列化
		if (type.getKind().isPrimitive()) return type.toString();

		return ProcessorUtils.toTypeElement(type).getQualifiedName().toString();
	}

	void error(String message, Element e) {
		processingEnv.get().getMessager().printMessage(Kind.ERROR, message, e);
	}
}
