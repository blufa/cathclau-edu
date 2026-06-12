package gn.lacaeducation.platform.application.port.in;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.Tenant;
import java.util.List;
public interface CreateTenantUseCase {
    Tenant create(CreateTenantCommand command);
    Tenant findById(Long id);
    TenantDetail getDetail(Long id);
    List<Tenant> findAll(String status, String city);
    Tenant update(Long id, UpdateTenantCommand command);
    void suspend(Long id);
    void activate(Long id);
    void delete(Long id);

    record CreateTenantCommand(String schoolName, String email, String phone,
            String address, String city, String country, String director) {}
    record UpdateTenantCommand(String schoolName, String logo, String brandColor,
            String address, String city, String country, String phone, String email,
            String website, String director) {}

    /** Tenant detail with its current subscription, plan and consumed student quota (§1.1). */
    record TenantDetail(Tenant tenant, Subscription currentSubscription, Plan plan,
            long consumedStudents, Integer studentLimit) {}
}
