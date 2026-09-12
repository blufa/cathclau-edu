package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily scheduled job that detects overdue installments.
 * For each installment whose due_date has passed and that is not yet PAID,
 * it marks it OVERDUE and emits an EcheanceDepasseeEvent so the communication
 * module can send reminders to parents.
 *
 * <p>S'exécute hors contexte de requête (pas de JWT, donc pas de {@link TenantContext} déjà résolu) :
 * boucle explicitement par tenant en restaurant {@link TenantContext}, comme
 * {@code AbsenceAutoMarkScheduler}. Sans cette boucle, le filtre Hibernate multi-tenant restait
 * désactivé (voir {@code TenantFilterAspect}), la recherche d'échéances en retard remontait TOUS les
 * tenants à la fois, et le traitement par {@code communication} (résolution des contacts parents, de la
 * config SMS du tenant…) n'avait aucun tenant sur lequel s'appuyer — d'où l'impression que les relances
 * SMS ne partaient que lorsqu'un utilisateur était connecté (seul cas où le filtre JWT pose déjà le
 * contexte tenant).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineCheckScheduler {

    private final TenantListPort tenantListPort;
    private final OverdueInstallmentProcessor processor;

    @Scheduled(cron = "0 0 6 * * *") // daily at 6am
    public void checkOverdueDeadlines() {
        LocalDate today = LocalDate.now();
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                processor.checkOverdueDeadlinesForTenant(tenant.id(), today);
            } catch (Exception e) {
                log.error("Overdue deadline check failed for tenant {}: {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
