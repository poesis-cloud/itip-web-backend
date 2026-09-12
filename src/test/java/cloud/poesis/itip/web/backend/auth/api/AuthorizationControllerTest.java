package cloud.poesis.itip.web.backend.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheckManyRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
import cloud.poesis.itip.web.backend.auth.service.AuthorizationService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthorizationControllerTest {

  @Mock private AuthorizationService authorizationService;

  @InjectMocks private AuthorizationController authorizationController;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void checkManyShouldMapAuthenticatedPrincipalAndReturnJson() throws Exception {
    UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            "authorized@itip.local", "ignored", List.of());
    AuthorizationCheck check =
        new AuthorizationCheck(
            CapabilityResourceOrigin.ITIP, "asset", CapabilityOperation.READ, resourceId);
    when(authorizationService.checkMany("authorized@itip.local", List.of(check)))
        .thenReturn(
            List.of(
                new AuthorizationDecision(
                    CapabilityResourceOrigin.ITIP,
                    "asset",
                    CapabilityOperation.READ,
                    resourceId,
                    true)));
    mockMvc()
        .perform(
            post("/api/authorization/check-many")
                .principal(Objects.requireNonNull(authentication))
                .contentType("application/json")
                .content(
                    """
                    {"checks":[{"origin":"ITIP","resource":"asset","operation":"READ","resourceId":"11111111-1111-1111-1111-111111111111"}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decisions[0].origin").value("ITIP"))
        .andExpect(jsonPath("$.decisions[0].resource").value("asset"))
        .andExpect(jsonPath("$.decisions[0].operation").value("READ"))
        .andExpect(jsonPath("$.decisions[0].resourceId").value(resourceId.toString()))
        .andExpect(jsonPath("$.decisions[0].allowed").value(true));

    verify(authorizationService).checkMany(eq("authorized@itip.local"), eq(List.of(check)));
  }

  @Test
  void checkManyShouldRejectEmptyChecks() throws Exception {
    mockMvc()
        .perform(
            post("/api/authorization/check-many")
                .contentType("application/json")
                .content("{\"checks\":[]}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(authorizationService);
  }

  @Test
  void checkManyShouldRejectAnonymousAuthentication() {
    AuthorizationCheckManyRequest request =
        new AuthorizationCheckManyRequest(
            List.of(
                new AuthorizationCheck(
                    CapabilityResourceOrigin.ITIP, "asset", CapabilityOperation.READ, null)));
    AnonymousAuthenticationToken authentication =
        new AnonymousAuthenticationToken("key", "anonymousUser", List.of(() -> "ROLE_ANONYMOUS"));

    assertThat(authorizationController.checkMany(authentication, request).getStatusCode().value())
        .isEqualTo(401);

    verifyNoInteractions(authorizationService);
  }

  private MockMvc mockMvc() {
    return MockMvcBuilders.standaloneSetup(authorizationController).build();
  }
}
