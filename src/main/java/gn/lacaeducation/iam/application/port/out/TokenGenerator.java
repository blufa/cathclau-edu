package gn.lacaeducation.iam.application.port.out;

import java.util.List;

public interface TokenGenerator {
    String generate(Long userId, Long tenantId, List<String> roles);
}
