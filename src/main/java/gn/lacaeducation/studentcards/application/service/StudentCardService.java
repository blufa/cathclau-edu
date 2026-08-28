package gn.lacaeducation.studentcards.application.service;

import gn.lacaeducation.academics.AcademicYearPublicInfo;
import gn.lacaeducation.academics.AcademicsDataProvider;
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
    private final AcademicsDataProvider academicsDataProvider;

    @Value("${app.qr-secret:lacaeducation-qr-secret}")
    private String qrSecret;

    /**
     * {@code photoUrl} est facultative : sans photo, la carte affiche les initiales de l'élève (voir
     * IdBadge côté front) — pas de blocage. {@code academicYearId}/{@code expirationDate} : si absents,
     * la carte est bornée à l'année scolaire courante (validité = l'année en cours uniquement).
     */
    public record IssueCardCommand(Long tenantId, Long studentId, Long cardTemplateId, Long academicYearId,
                                   Long classId, String studentNumber, String photoUrl, LocalDate expirationDate) {}

    public StudentCard issue(IssueCardCommand c) {
        if (repo.findByStudentIdAndStatus(c.studentId(), CardStatus.ACTIVE).isPresent()) {
            throw new ConflictException("Une carte active existe déjà pour cet élève (utilisez la réémission)");
        }
        return create(withCurrentYear(c), null);
    }

    /** Complète l'année scolaire courante (id + fin) quand l'appelant ne l'a pas fournie explicitement. */
    private IssueCardCommand withCurrentYear(IssueCardCommand c) {
        if (c.academicYearId() != null && c.expirationDate() != null) return c;
        AcademicYearPublicInfo year = academicsDataProvider.currentAcademicYear(c.tenantId()).orElse(null);
        if (year == null) return c;
        return new IssueCardCommand(c.tenantId(), c.studentId(), c.cardTemplateId(),
                c.academicYearId() != null ? c.academicYearId() : year.id(),
                c.classId(), c.studentNumber(), c.photoUrl(),
                c.expirationDate() != null ? c.expirationDate() : year.endDate());
    }

    public StudentCard replace(Long id) {
        StudentCard old = load(id);
        old.setStatus(CardStatus.REPLACED);
        repo.save(old);
        // Réémission = maintenant : on rebascule sur l'année scolaire courante plutôt que de reconduire
        // l'ancienne validité (une carte perdue puis réémise reste valable l'année en cours).
        IssueCardCommand refreshed = new IssueCardCommand(old.getTenantId(), old.getStudentId(), old.getCardTemplateId(),
                null, old.getClassId(), old.getStudentNumber(), old.getPhotoUrl(), null);
        return create(withCurrentYear(refreshed), old.getId());
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

    /** Numéro de carte au format simple {@code SIGLESEQ} (ex. {@code AT00001}) — comme le matricule élève. */
    private String nextCardNumber(Long tenantId) {
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        long seq = repo.countByTenantId(tenantId) + 1;
        return String.format("%s%05d", prefix, seq);
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
