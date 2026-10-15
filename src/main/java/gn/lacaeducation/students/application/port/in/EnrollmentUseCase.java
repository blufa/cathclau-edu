package gn.lacaeducation.students.application.port.in;

import gn.lacaeducation.students.EnrollmentType;
import gn.lacaeducation.students.domain.*;
import java.math.BigDecimal;
import java.util.List;

public interface EnrollmentUseCase {
    /** platformAccessOptIn : le parent accepte-t-il que l'élève soit connecté à la plateforme (frais
     * annexe ajouté aux mensualités si oui) ? Null/absent traité comme false.
     * monthlyPaymentOptIn : ce parent règle-t-il mois par mois plutôt que par tranche groupée (voir
     * Enrollment#monthlyPaymentOptIn) ? Null/absent traité comme false. */
    record EnrollCommand(Long studentId, Long classId, Long academicYearId, EnrollmentType type,
                         Boolean platformAccessOptIn, Boolean monthlyPaymentOptIn) {}

    /** Dette (factures UNPAID/PARTIAL hors l'année {@code excludingAcademicYearId}) d'un élève —
     * affichée sur l'écran de réinscription pour expliquer un blocage (voir {@link #reenroll}). */
    record ArrearsSummary(Long studentId, BigDecimal totalDue, int invoiceCount) {}

    Enrollment enroll(EnrollCommand command);
    /** Réinscrit l'élève — refuse (BusinessRuleException) s'il a des factures impayées d'une année
     * antérieure à {@code command.academicYearId()}, même s'il est académiquement admis. Voir
     * {@link #arrears} pour afficher ce montant en amont côté écran de réinscription. */
    Enrollment reenroll(EnrollCommand command);
    /** Dette de chaque élève listé, hors l'année {@code excludingAcademicYearId} — pour l'écran de
     * réinscription en masse (une entrée par studentId, {@code totalDue} = 0 si aucune dette). */
    List<ArrearsSummary> arrears(List<Long> studentIds, Long excludingAcademicYearId);
    Enrollment update(Long id, EnrollCommand command);
    /** Modifie après coup le choix d'accès à la plateforme (le parent peut changer d'avis en cours d'année). */
    Enrollment updatePlatformAccessOptIn(Long id, boolean optIn);
    /** Modifie après coup le choix de paiement mensuel (le parent peut changer d'avis en cours d'année). */
    Enrollment updateMonthlyPaymentOptIn(Long id, boolean optIn);
    Enrollment validate(Long id);
    Enrollment cancel(Long id);
    /** Marque une inscription validée comme abandonnée (élève parti en cours d'année) — distinct de
     * {@link #cancel}, qui reste réservé aux annulations (n'a jamais vraiment commencé). */
    Enrollment abandon(Long id);
    /** Transfère une inscription validée vers une autre classe en cours d'année (frais scolaires
     * potentiellement différents) — publie {@code EnrollmentClassChangedEvent}, consommé par billing
     * pour annuler les mensualités de l'ancienne classe et reporter le crédit déjà payé sur les
     * nouvelles mensualités. */
    Enrollment transferClass(Long id, Long newClassId);
    Enrollment findById(Long id);
    List<Enrollment> findByStudent(Long studentId);
    List<Enrollment> findByClass(Long classId);
    List<Enrollment> findByAcademicYear(Long academicYearId);
    List<Enrollment> search(Long studentId, Long classId, Long academicYearId, EnrollmentStatus status);
    void delete(Long id);
}
