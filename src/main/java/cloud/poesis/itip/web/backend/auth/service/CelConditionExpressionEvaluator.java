package cloud.poesis.itip.web.backend.auth.service;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelKind;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CelConditionExpressionEvaluator implements ConditionExpressionEvaluator {

  private final CelCompiler compiler =
      CelCompilerFactory.standardCelCompilerBuilder()
          .addVar("actor", SimpleType.DYN)
          .addVar("target", SimpleType.DYN)
          .setResultType(SimpleType.BOOL)
          .build();
  private final CelRuntime runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();

  @Override
  public boolean evaluate(String expression, Map<String, ?> variables) {
    if (expression == null || expression.isBlank() || variables == null) {
      return false;
    }

    try {
      CelValidationResult validation = compiler.compile(expression);
      if (validation.hasError()) {
        return false;
      }
      CelAbstractSyntaxTree ast = validation.getAst();
      if (ast.getResultType().kind() != CelKind.BOOL) {
        return false;
      }
      Object result = runtime.createProgram(ast).eval(variables);
      return Boolean.TRUE.equals(result);
    } catch (Exception exception) {
      return false;
    }
  }

  public boolean isValid(String expression) {
    if (expression == null || expression.isBlank()) {
      return false;
    }
    try {
      CelValidationResult validation = compiler.compile(expression);
      return !validation.hasError() && validation.getAst().getResultType().kind() == CelKind.BOOL;
    } catch (Exception exception) {
      return false;
    }
  }
}
