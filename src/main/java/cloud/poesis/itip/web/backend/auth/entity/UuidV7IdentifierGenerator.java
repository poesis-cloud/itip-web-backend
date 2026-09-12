package cloud.poesis.itip.web.backend.auth.entity;

import java.lang.reflect.Member;
import java.util.EnumSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;

public class UuidV7IdentifierGenerator
    implements BeforeExecutionGenerator, AnnotationBasedGenerator<GenerateUuidV7> {

  @Override
  public void initialize(
      GenerateUuidV7 configuration, Member member, GeneratorCreationContext creationContext) {}

  @Override
  public Object generate(
      SharedSessionContractImplementor session,
      Object object,
      Object currentValue,
      EventType eventType) {
    return UuidV7Generator.generate();
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EventTypeSets.INSERT_ONLY;
  }
}
