package cloud.poesis.itip.web.backend.defman;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DefmanAscription(UUID id, JsonNode statement, Instant timestamp, String status) {}
