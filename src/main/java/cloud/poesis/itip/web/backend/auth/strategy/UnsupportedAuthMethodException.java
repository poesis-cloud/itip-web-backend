package cloud.poesis.itip.web.backend.auth.strategy;

public class UnsupportedAuthMethodException extends RuntimeException {

  public UnsupportedAuthMethodException(String message) {
    super(message);
  }
}
