package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.RecessWindow;

import java.util.List;

public interface RecessWindowRepository {
    List<RecessWindow> findByTenantId(Long tenantId);
    RecessWindow save(RecessWindow window);
    void deleteById(Long id);
}
