package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.application.port.in.ScheduledBroadcastUseCase;
import gn.lacaeducation.communication.application.port.out.ScheduledBroadcastRepository;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.ScheduledBroadcast;
import gn.lacaeducation.communication.domain.ScheduledBroadcastStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledBroadcastService implements ScheduledBroadcastUseCase {

    private final ScheduledBroadcastRepository repository;
    private final BroadcastUseCase broadcastUseCase;

    @Override
    public ScheduledBroadcast schedule(ScheduleCommand c) {
        if (c.sendAt() == null) {
            throw new BusinessRuleException("La date/heure d'envoi est obligatoire.");
        }
        if (c.sendAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("La date/heure d'envoi doit être dans le futur.");
        }
        boolean needsClass = c.audience() == BroadcastAudience.CLASS_PARENTS
                || c.audience() == BroadcastAudience.CLASS_PARENTS_BOYS
                || c.audience() == BroadcastAudience.CLASS_PARENTS_GIRLS;
        if (needsClass && c.classId() == null) {
            throw new BusinessRuleException("classId est requis pour l'audience " + c.audience());
        }
        if (c.audience() == BroadcastAudience.STUDENT_PARENTS && c.studentId() == null) {
            throw new BusinessRuleException("studentId est requis pour l'audience STUDENT_PARENTS");
        }

        ScheduledBroadcast broadcast = ScheduledBroadcast.builder()
                .tenantId(c.tenantId())
                .senderId(c.senderId())
                .channel(c.channel())
                .audience(c.audience())
                .classId(c.classId())
                .studentId(c.studentId())
                .subject(c.subject())
                .content(c.content())
                .reason(c.reason())
                .sendAt(c.sendAt())
                .schoolEventId(c.schoolEventId())
                .status(ScheduledBroadcastStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(broadcast);
    }

    @Override
    public List<ScheduledBroadcast> findByTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public void cancel(Long id) {
        ScheduledBroadcast broadcast = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Diffusion programmée introuvable."));
        if (broadcast.getStatus() != ScheduledBroadcastStatus.PENDING) {
            throw new BusinessRuleException("Cette diffusion n'est plus en attente, elle ne peut plus être annulée.");
        }
        broadcast.setStatus(ScheduledBroadcastStatus.CANCELLED);
        repository.save(broadcast);
    }

    /** Déclenche toutes les diffusions dues pour un tenant — appelé par le job planifié, tenant par
     * tenant (voir {@code ScheduledBroadcastDispatchScheduler}). Une diffusion en échec (aucun
     * destinataire joignable, ou erreur d'envoi) n'empêche pas le traitement des suivantes. */
    @Override
    @Transactional
    public void processDueForTenant(Long tenantId, LocalDateTime now) {
        for (ScheduledBroadcast broadcast : repository.findDuePending(tenantId, now)) {
            try {
                Communication result = broadcastUseCase.broadcast(new BroadcastUseCase.BroadcastCommand(
                        broadcast.getTenantId(), broadcast.getSenderId(), broadcast.getChannel(),
                        broadcast.getAudience(), broadcast.getClassId(), broadcast.getStudentId(),
                        broadcast.getSubject(), broadcast.getContent(), broadcast.getReason()));
                if (result == null) {
                    broadcast.setStatus(ScheduledBroadcastStatus.FAILED);
                    broadcast.setErrorMessage("Aucun destinataire joignable pour cette audience.");
                } else {
                    broadcast.setStatus(ScheduledBroadcastStatus.SENT);
                    broadcast.setSentAt(now);
                }
            } catch (Exception e) {
                log.error("Diffusion programmée id={} en échec : {}", broadcast.getId(), e.getMessage());
                broadcast.setStatus(ScheduledBroadcastStatus.FAILED);
                broadcast.setErrorMessage(e.getMessage());
            }
            repository.save(broadcast);
        }
    }
}
