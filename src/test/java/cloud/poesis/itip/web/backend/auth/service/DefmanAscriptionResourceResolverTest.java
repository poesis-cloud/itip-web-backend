package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.defman.DefmanAscription;
import cloud.poesis.itip.web.backend.defman.DefmanClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefmanAscriptionResourceResolverTest {

  @Mock private DefmanClient defmanClient;

  private DefmanAscriptionResourceResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new DefmanAscriptionResourceResolver(defmanClient, new ObjectMapper());
  }

  @Test
  void supportsOnlyDefmanAscriptions() {
    assertThat(resolver.supports(PrivilegeResourceOrigin.DEFMAN, "ASCRIPTION")).isTrue();
    assertThat(resolver.supports(PrivilegeResourceOrigin.ITIP, "ASCRIPTION")).isFalse();
    assertThat(resolver.supports(PrivilegeResourceOrigin.DEFMAN, "STRUCTURE")).isFalse();
  }

  @Test
  void mapsDefmanAscriptionIntoCelTarget() throws Exception {
    UUID id = UUID.randomUUID();
    when(defmanClient.getAscription(id))
        .thenReturn(
            new DefmanAscription(
                id, new ObjectMapper().readTree("{\"ownerId\":\"actor-1\"}"), null, "ACTIVE"));

    var target = resolver.resolve(PrivilegeResourceOrigin.DEFMAN, "ASCRIPTION", id).orElseThrow();

    assertThat(target)
        .containsEntry("id", id.toString())
        .containsEntry("status", "ACTIVE")
        .containsKey("statement")
        .containsEntry("ownerId", "actor-1");
    assertThat(target.get("statement")).isEqualTo(java.util.Map.of("ownerId", "actor-1"));
  }

  @Test
  void returnsEmptyWhenDefmanCannotResolveTarget() {
    UUID id = UUID.randomUUID();
    when(defmanClient.getAscription(id)).thenThrow(new IllegalStateException("unavailable"));

    assertThat(resolver.resolve(PrivilegeResourceOrigin.DEFMAN, "ASCRIPTION", id)).isEmpty();
  }
}
