package cloud.poesis.itip.web.backend;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ItipWebBackendApplicationMainTest {

  @Test
  void mainDelegatesToSpringApplicationRun() {
    String[] args = {"--spring.main.web-application-type=none"};

    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      ItipWebBackendApplication.main(args);

      springApplication.verify(() -> SpringApplication.run(ItipWebBackendApplication.class, args));
    }
  }
}
