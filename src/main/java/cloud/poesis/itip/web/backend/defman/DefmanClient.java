package cloud.poesis.itip.web.backend.defman;

import java.util.UUID;

public interface DefmanClient {

  DefmanAscription getAscription(UUID id);
}
