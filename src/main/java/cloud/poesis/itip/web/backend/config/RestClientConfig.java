package cloud.poesis.itip.web.backend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Bounds outbound REST client timeouts so a slow or half-open downstream (Defman) cannot hold
 * request threads indefinitely. Applied via {@link RestClientCustomizer}, which Spring Boot's
 * autoconfigured {@code RestClient.Builder} bean picks up automatically at runtime; unit tests
 * construct their own {@code RestClient.Builder} directly and are unaffected.
 */
@Configuration
public class RestClientConfig {

  @Bean
  RestClientCustomizer defmanTimeoutCustomizer(
      @Value("${itip.defman.connect-timeout-ms:5000}") long connectTimeoutMs,
      @Value("${itip.defman.read-timeout-ms:10000}") long readTimeoutMs) {
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactoryBuilder.detect()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .withReadTimeout(Duration.ofMillis(readTimeoutMs)));
    return builder -> builder.requestFactory(requestFactory);
  }
}
