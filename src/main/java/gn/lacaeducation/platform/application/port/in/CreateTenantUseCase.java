package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.Tenant;

import java.util.List;

public interface CreateTenantUseCase {
    Tenant create(CreateTenantCommand command);
    Tenant findById(Long id);
    List<Tenant> findAll();
    Tenant update(Long id, UpdateTenantCommand command);
    void suspend(Long tenantId);

    record CreateTenantCommand(
        String schoolName,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String director
    ) {}

    record UpdateTenantCommand(
        String schoolName,
        String logo,
        String brandColor,
        String address,
        String city,
        String country,
        String phone,
        String email,
        String website,
        String director
    ) {}
}
