package cloud.poesis.itip.web.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;

/**
 * Verifies that {@link RestClientConfig#defmanTimeoutCustomizer(long, long)} actually produces a
 * bounded read timeout on the {@link RestClient} it customizes, so a slow/half-open downstream
 * cannot hold the calling thread indefinitely. This exercises real socket I/O rather than
 * inspecting the request-factory internals, since the concrete implementation returned by {@code
 * ClientHttpRequestFactoryBuilder.detect()} depends on what HTTP client libraries are on the
 * classpath.
 */
class RestClientConfigTest {

  private ServerSocket serverSocket;

  @AfterEach
  void tearDown() throws IOException {
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
  }

  @Test
  void appliesConfiguredReadTimeoutToCustomizedRestClient() throws IOException {
    serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();
    Thread acceptingSilentServer =
        new Thread(
            () -> {
              try (Socket socket = serverSocket.accept()) {
                // Accept the connection but never write a response, forcing the client to wait
                // on the socket read until its configured read timeout elapses.
                Thread.sleep(Duration.ofSeconds(5).toMillis());
              } catch (IOException | InterruptedException ignored) {
                // Server thread ends once the test's client-side timeout fires and closes the
                // socket, or the test itself tears down.
              }
            });
    acceptingSilentServer.setDaemon(true);
    acceptingSilentServer.start();

    RestClientCustomizer customizer = new RestClientConfig().defmanTimeoutCustomizer(5000, 300);
    RestClient.Builder builder = RestClient.builder();
    customizer.customize(builder);
    RestClient restClient = builder.baseUrl("http://localhost:" + port).build();

    Instant start = Instant.now();
    assertThatThrownBy(() -> restClient.get().uri("/probe").retrieve().body(String.class))
        .isInstanceOf(RuntimeException.class);
    Duration elapsed = Duration.between(start, Instant.now());

    assertThat(elapsed).isLessThan(Duration.ofSeconds(4));
  }
}
