package cloud.poesis.itip.web.backend.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UuidV7EntityLifecycleTest {

  @Test
  void shouldGenerateUuidV7Identifiers() {
    java.util.UUID id = UuidV7Generator.generate();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
  }
}
