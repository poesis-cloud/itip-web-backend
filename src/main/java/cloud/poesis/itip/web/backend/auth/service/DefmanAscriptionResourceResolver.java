package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.defman.DefmanAscription;
import cloud.poesis.itip.web.backend.defman.DefmanClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefmanAscriptionResourceResolver implements AuthorizationResourceResolver {

  private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {};

  private final DefmanClient defmanClient;
  private final ObjectMapper objectMapper;

  public DefmanAscriptionResourceResolver(DefmanClient defmanClient, ObjectMapper objectMapper) {
    this.defmanClient = defmanClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(CapabilityResourceOrigin origin, String resource) {
    return origin == CapabilityResourceOrigin.DEFMAN && "ASCRIPTION".equals(resource);
  }

  @Override
  public Optional<Map<String, Object>> resolve(
      CapabilityResourceOrigin origin, String resource, UUID resourceId) {
    if (!supports(origin, resource) || resourceId == null) {
      return Optional.empty();
    }
    try {
      DefmanAscription ascription = defmanClient.getAscription(resourceId);
      if (ascription == null || ascription.id() == null || ascription.statement() == null) {
        return Optional.empty();
      }
      Map<String, Object> target = new LinkedHashMap<>();
      target.put("id", ascription.id().toString());
      target.put("status", ascription.status());
      target.put("timestamp", ascription.timestamp());
      Map<String, Object> statement = objectMapper.convertValue(ascription.statement(), OBJECT_MAP);
      target.put("statement", statement);
      statement.forEach(target::putIfAbsent);
      return Optional.of(target);
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }
}
