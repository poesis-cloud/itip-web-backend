package cloud.poesis.itip.web.backend.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(
    properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.liquibase.enabled=false"})
class UuidV7EntityLifecycleTest {

  @Autowired private TestEntityManager entityManager;

  @Test
  void shouldGenerateUuidV7IdentifiersForPersistedEntities() {
    Account account =
        entityManager.persistFlushFind(
            Account.builder()
                .email("uuid-v7@itip.local")
                .passwordHash("hashed-password")
                .enabled(true)
                .build());

    assertThat(account.getId()).isNotNull();
    assertThat(account.getId().version()).isEqualTo(7);
    assertThat(account.getId().variant()).isEqualTo(2);
  }
}
