package cloud.poesis.itip.web.backend.auth.strategy;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;

public interface AuthenticationStrategy {

  AuthenticationResult authenticate(AuthenticationRequest request);

  boolean supports(AuthMethod authMethod);
}
