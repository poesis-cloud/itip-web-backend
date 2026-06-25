package cloud.poesis.itip.web.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserEntityTest {

  @Test
  void constructorAndSettersPopulateFields() {
    UserEntity user = new UserEntity("test.user", "Test", "User");

    assertThat(user.getId()).isNull();
    assertThat(user.getUsername()).isEqualTo("test.user");
    assertThat(user.getFirstName()).isEqualTo("Test");
    assertThat(user.getLastName()).isEqualTo("User");

    user.setUsername("updated.user");
    user.setFirstName("Updated");
    user.setLastName("Name");

    assertThat(user.getUsername()).isEqualTo("updated.user");
    assertThat(user.getFirstName()).isEqualTo("Updated");
    assertThat(user.getLastName()).isEqualTo("Name");
  }
}
