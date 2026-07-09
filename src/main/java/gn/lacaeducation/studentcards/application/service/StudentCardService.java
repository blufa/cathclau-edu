package gn.lacaeducation.studentcards.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.studentcards.application.port.out.StudentCardRepository;
import gn.lacaeducation.studentcards.domain.CardStatus;
import gn.lacaeducation.studentcards.domain.StudentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

/**
 * Student cards (features/SCHOOL_back_complements.md §L). A single ACTIVE card per student; re-issue
 * marks the previous one REPLACED. The QR payload is HMAC-signed so a scan can be verified server-side.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudentCardService {

    private final StudentCardRepository repo;
    private final TenantInfoProvider tenantInfoProvider;

    @Value("${app.qr-secret:lacaeducation-qr-secret}")
    private String qrSecret;

    public record IssueCardCommand(Long tenantId, Long studentId, Long cardTemplateId, Long academicYearId,
                                   Long classId, String studentNumber, String photoUrl, LocalDate expirationDate) {}

    public StudentCard issue(IssueCardCommand c) {
        if (c.photoUrl() == null || c.photoUrl().isBlank()) {
            throw new BusinessRuleException("La photo de l'élève est requise pour générer la carte");
        }
        if (repo.findByStudentIdAndStatus(c.studentId(), CardStatus.ACTIVE).isPresent()) {
            throw new ConflictException("Une carte active existe déjà pour cet élève (utilisez la réémission)");
        }
        return create(c, null);
    }

    public StudentCard replace(Long id) {
        StudentCard old = load(id);
        old.setStatus(CardStatus.REPLACED);
        repo.save(old);
        return create(new IssueCardCommand(old.getTenantId(), old.getStudentId(), old.getCardTemplateId(),
                old.getAcademicYearId(), old.getClassId(), old.getStudentNumber(), old.getPhotoUrl(), old.getExpirationDate()), old.getId());
    }

    public StudentCard declareLost(Long id) {
        StudentCard c = load(id);
        c.setStatus(CardStatus.LOST);
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public List<StudentCard> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public Optional<StudentCard> currentCard(Long studentId) { return repo.findByStudentIdAndStatus(studentId, CardStatus.ACTIVE); }

    @Transactional(readOnly = true)
    public StudentCard findById(Long id) { return load(id); }

    /** Verifies a scanned QR payload/signature; returns the card when valid & ACTIVE. */
    @Transactional(readOnly = true)
    public Optional<StudentCard> verify(String payload, String signature) {
        if (payload == null || signature == null || !sign(payload).equals(signature)) return Optional.empty();
        return repo.findByQrPayload(payload).filter(c -> c.getStatus() == CardStatus.ACTIVE);
    }

    private StudentCard create(IssueCardCommand c, Long replacesId) {
        String cardNumber = nextCardNumber(c.tenantId());
        String payload = String.format("LACA|%d|%d|%s|%s|%s", c.tenantId(), c.studentId(),
                c.studentNumber() != null ? c.studentNumber() : "", cardNumber, Year.now());
        return repo.save(StudentCard.builder()
                .tenantId(c.tenantId()).studentId(c.studentId()).cardTemplateId(c.cardTemplateId())
                .academicYearId(c.academicYearId()).classId(c.classId()).studentNumber(c.studentNumber())
                .cardNumber(cardNumber).qrPayload(payload).qrSignature(sign(payload)).photoUrl(c.photoUrl())
                .issueDate(LocalDate.now()).expirationDate(c.expirationDate()).status(CardStatus.ACTIVE)
                .replacesCardId(replacesId).build());
    }

    /** Numéro de carte au format {@code SIGLE-CRT-YEAR-SEQ} (ex. {@code GSC-CRT-2026-000042}). */
    private String nextCardNumber(Long tenantId) {
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        long seq = repo.countByTenantId(tenantId) + 1;
        return String.format("%s-CRT-%s-%06d", prefix, Year.now(), seq);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign QR payload", e);
        }
    }

    private StudentCard load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Carte introuvable : " + id)); }
}
