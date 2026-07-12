package cloud.poesis.itip.web.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles({"test", "local"})
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "itip.security.permit-all=true")
class LocalPermissiveSecurityConfigTest {

  private final MockMvc mockMvc;

  @Autowired
  LocalPermissiveSecurityConfigTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void localPermitAllModeAllowsUnauthenticatedApplicationEndpoints() throws Exception {
    mockMvc
        .perform(get("/security-test/local-protected"))
        .andExpect(status().isOk())
        .andExpect(content().string("local-protected"));
  }

  @TestConfiguration
  static class LocalTestEndpointConfig {

    @RestController
    static class LocalProtectedEndpoint {

      @GetMapping("/security-test/local-protected")
      String protectedEndpoint() {
        return "local-protected";
      }
    }
  }
}
