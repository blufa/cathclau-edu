package gn.lacaeducation.studentcards.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.studentcards.application.port.out.StudentCardRepository;
import gn.lacaeducation.studentcards.domain.CardStatus;
import gn.lacaeducation.studentcards.domain.StudentCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCardServiceTest {

    @Mock StudentCardRepository repo;
    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock AcademicsDataProvider academicsDataProvider;

    private StudentCardService service() {
        StudentCardService s = new StudentCardService(repo, tenantInfoProvider, academicsDataProvider);
        ReflectionTestUtils.setField(s, "qrSecret", "test-secret");
        return s;
    }

    private String sign(StudentCardService s, String payload) {
        return (String) ReflectionTestUtils.invokeMethod(s, "sign", payload);
    }

    @Test
    void verify_activeCardNotYetExpired_isValid() {
        StudentCardService s = service();
        String payload = "LACA|1|7|MAT-1|CARD-1|2026";
        String signature = sign(s, payload);
        StudentCard card = StudentCard.builder().id(1L).status(CardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusDays(1)).qrPayload(payload).build();
        when(repo.findByQrPayload(payload)).thenReturn(Optional.of(card));

        Optional<StudentCard> result = s.verify(payload, signature);

        assertThat(result).isPresent();
    }

    @Test
    void verify_activeCardPastExpirationDate_isInvalid() {
        // Le statut est toujours ACTIVE en base (aucune tâche planifiée ne l'a encore basculé sur
        // EXPIRED) : la vérification doit quand même rejeter une carte dont la date est dépassée.
        StudentCardService s = service();
        String payload = "LACA|1|7|MAT-1|CARD-1|2026";
        String signature = sign(s, payload);
        StudentCard card = StudentCard.builder().id(1L).status(CardStatus.ACTIVE)
                .expirationDate(LocalDate.now().minusDays(1)).qrPayload(payload).build();
        when(repo.findByQrPayload(payload)).thenReturn(Optional.of(card));

        Optional<StudentCard> result = s.verify(payload, signature);

        assertThat(result).isEmpty();
    }

    @Test
    void verify_cardWithoutExpirationDate_isValid() {
        StudentCardService s = service();
        String payload = "LACA|1|7|MAT-1|CARD-1|2026";
        String signature = sign(s, payload);
        StudentCard card = StudentCard.builder().id(1L).status(CardStatus.ACTIVE)
                .expirationDate(null).qrPayload(payload).build();
        when(repo.findByQrPayload(payload)).thenReturn(Optional.of(card));

        Optional<StudentCard> result = s.verify(payload, signature);

        assertThat(result).isPresent();
    }

    @Test
    void verify_wrongSignature_isInvalid() {
        StudentCardService s = service();
        Optional<StudentCard> result = s.verify("LACA|1|7|MAT-1|CARD-1|2026", "bad-signature");
        assertThat(result).isEmpty();
    }

    @Test
    void verify_nonActiveStatus_isInvalid() {
        StudentCardService s = service();
        String payload = "LACA|1|7|MAT-1|CARD-1|2026";
        String signature = sign(s, payload);
        StudentCard card = StudentCard.builder().id(1L).status(CardStatus.LOST)
                .expirationDate(LocalDate.now().plusDays(1)).qrPayload(payload).build();
        when(repo.findByQrPayload(payload)).thenReturn(Optional.of(card));

        Optional<StudentCard> result = s.verify(payload, signature);

        assertThat(result).isEmpty();
    }

    // ── getOrRefresh (§retour utilisateur, production : "j'ai modifié le matricule, mais le badge
    // garde l'ancien") — QuickBadgeButton n'appelle plus jamais une carte existante sans vérifier
    // qu'elle reflète encore les données courantes. ────────────────────────────────────────────────

    @Test
    void getOrRefresh_noActiveCard_createsNew() {
        StudentCardService s = service();
        when(repo.findByStudentIdAndStatus(7L, CardStatus.ACTIVE)).thenReturn(Optional.empty());
        when(repo.countByTenantId(1L)).thenReturn(0L);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        StudentCard result = s.getOrRefresh(new StudentCardService.IssueCardCommand(
                1L, 7L, null, null, 20L, "MAT-NEW", null, null));

        assertThat(result.getStudentNumber()).isEqualTo("MAT-NEW");
        assertThat(result.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void getOrRefresh_activeCardAlreadyUpToDate_returnsUnchangedWithoutNewCard() {
        StudentCardService s = service();
        StudentCard active = StudentCard.builder().id(5L).studentId(7L).tenantId(1L)
                .studentNumber("MAT-1").photoUrl("photo.jpg").classId(20L).status(CardStatus.ACTIVE).build();
        when(repo.findByStudentIdAndStatus(7L, CardStatus.ACTIVE)).thenReturn(Optional.of(active));

        StudentCard result = s.getOrRefresh(new StudentCardService.IssueCardCommand(
                1L, 7L, null, null, 20L, "MAT-1", "photo.jpg", null));

        assertThat(result).isSameAs(active);
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOrRefresh_studentNumberChangedSinceIssue_replacesCardWithFreshNumber() {
        // Reproduit exactement l'incident : le matricule a été modifié après l'émission de la carte —
        // cliquer "Badge" doit régénérer, jamais garder l'ancien matricule.
        StudentCardService s = service();
        StudentCard stale = StudentCard.builder().id(5L).studentId(7L).tenantId(1L)
                .studentNumber("MAT-OLD").photoUrl("photo.jpg").classId(20L).status(CardStatus.ACTIVE).build();
        when(repo.findByStudentIdAndStatus(7L, CardStatus.ACTIVE)).thenReturn(Optional.of(stale));
        when(repo.countByTenantId(1L)).thenReturn(1L);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        StudentCard result = s.getOrRefresh(new StudentCardService.IssueCardCommand(
                1L, 7L, null, null, 20L, "MAT-NEW", "photo.jpg", null));

        assertThat(result).isNotSameAs(stale);
        assertThat(result.getStudentNumber()).isEqualTo("MAT-NEW");
        assertThat(result.getReplacesCardId()).isEqualTo(5L);
        assertThat(stale.getStatus()).isEqualTo(CardStatus.REPLACED);
    }
}
