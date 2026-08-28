package cloud.poesis.itip.web.backend.defman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RestDefmanClientTest {

  private RestDefmanClient client;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestDefmanClient(builder, "http://defman.test");
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getsAnAscriptionFromDefman() {
    UUID id = UUID.randomUUID();
    MockHttpServletRequest incoming = new MockHttpServletRequest();
    incoming.addHeader("Authorization", "Bearer user-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incoming));
    server
        .expect(requestTo("http://defman.test/api/v1/ascriptions/" + id))
        .andExpect(
            request -> {
              if (request.getMethod() != GET) {
                throw new AssertionError("Expected GET request");
              }
            })
        .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
        .andExpect(header("Authorization", "Bearer user-token"))
        .andRespond(
            withSuccess(
                "{\"id\":\""
                    + id
                    + "\",\"statement\":{\"ownerId\":\"actor-1\"},"
                    + "\"timestamp\":\"2026-08-28T10:00:00Z\",\"status\":\"ACTIVE\"}",
                MediaType.APPLICATION_JSON));

    DefmanAscription result = client.getAscription(id);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.statement().get("ownerId").asText()).isEqualTo("actor-1");
    assertThat(result.status()).isEqualTo("ACTIVE");
    server.verify();
  }
}
