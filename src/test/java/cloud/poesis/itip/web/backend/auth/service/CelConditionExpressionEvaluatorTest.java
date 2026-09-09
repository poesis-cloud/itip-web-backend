package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CelConditionExpressionEvaluatorTest {

  private CelConditionExpressionEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new CelConditionExpressionEvaluator();
  }

  @Test
  void evaluatesBooleanExpressionAgainstActorAndTarget() {
    String actorId = "actor-1";

    assertThat(
            evaluator.evaluate(
                "target.ownerId != actor.id",
                Map.of("actor", Map.of("id", actorId), "target", Map.of("ownerId", "actor-2"))))
        .isTrue();
  }

  @Test
  void returnsFalseWhenConditionIsFalse() {
    assertThat(
            evaluator.evaluate(
                "target.ownerId == actor.id",
                Map.of("actor", Map.of("id", "actor-1"), "target", Map.of("ownerId", "actor-2"))))
        .isFalse();
  }

  @Test
  void rejectsInvalidExpression() {
    assertThat(evaluator.isValid("target.ownerId ==")).isFalse();
    assertThat(evaluator.evaluate("target.ownerId ==", Map.of())).isFalse();
  }

  @Test
  void rejectsNonBooleanExpression() {
    assertThat(evaluator.isValid("target.ownerId")).isFalse();
    assertThat(evaluator.evaluate("target.ownerId", Map.of("target", Map.of("ownerId", "x"))))
        .isFalse();
  }

  @Test
  void rejectsNullAndBlankExpressionsConsistentlyWithEvaluate() {
    assertThat(evaluator.isValid(null)).isFalse();
    assertThat(evaluator.isValid("")).isFalse();
    assertThat(evaluator.isValid("   ")).isFalse();
    assertThat(evaluator.evaluate(null, Map.of())).isFalse();
    assertThat(evaluator.evaluate("", Map.of())).isFalse();
  }
}
