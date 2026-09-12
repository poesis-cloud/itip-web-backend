package cloud.poesis.itip.web.backend.defman;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RestDefmanClient implements DefmanClient {

  private final RestClient restClient;
  private final String baseUrl;

  public RestDefmanClient(
      RestClient.Builder builder, @Value("${itip.defman.base-url:}") String baseUrl) {
    this.baseUrl = baseUrl;
    this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
  }

  @Override
  public DefmanAscription getAscription(UUID id) {
    if (baseUrl.isBlank()) {
      throw new IllegalStateException("Defman base URL is not configured");
    }
    var request =
        restClient
            .get()
            .uri("/api/v1/ascriptions/{id}", id)
            .header(HttpHeaders.ACCEPT, "application/json");
    var attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
      if (authorization != null && !authorization.isBlank()) {
        request.header(HttpHeaders.AUTHORIZATION, authorization);
      }
    }
    return request.retrieve().body(DefmanAscription.class);
  }

  public String baseUrl() {
    return baseUrl;
  }
}
