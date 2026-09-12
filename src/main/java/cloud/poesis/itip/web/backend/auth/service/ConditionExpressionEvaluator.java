package cloud.poesis.itip.web.backend.auth.service;

import java.util.Map;

public interface ConditionExpressionEvaluator {

  boolean evaluate(String expression, Map<String, ?> variables);

  boolean isValid(String expression);
}
