package gn.lacaeducation.communication.application.service;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.communication.application.port.in.MessageTemplateUseCase;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.MessageTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service @Transactional @RequiredArgsConstructor
public class MessageTemplateService implements MessageTemplateUseCase {
    private final MessageTemplateRepository repo;

    @Override public MessageTemplate create(SaveTemplateCommand c) {
        repo.findByTenantIdAndReason(c.tenantId(), c.reason()).ifPresent(t -> {
            throw new ConflictException("A template already exists for reason " + c.reason());
        });
        return repo.save(MessageTemplate.builder().tenantId(c.tenantId()).reason(c.reason())
                .channel(c.channel()).subject(c.subject()).body(c.body())
                .createdAt(LocalDateTime.now()).build());
    }

    @Override public MessageTemplate update(Long id, SaveTemplateCommand c) {
        MessageTemplate t = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("MessageTemplate", id));
        repo.findByTenantIdAndReason(c.tenantId(), c.reason())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("A template already exists for reason " + c.reason()); });
        t.setReason(c.reason());
        t.setChannel(c.channel());
        t.setSubject(c.subject());
        t.setBody(c.body());
        return repo.save(t);
    }

    @Override @Transactional(readOnly = true) public Optional<MessageTemplate> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly = true) public List<MessageTemplate> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override public void delete(Long id) { repo.deleteById(id); }
}
