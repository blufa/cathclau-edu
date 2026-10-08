package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.attendance.application.event.StudentExitEvent;
import gn.lacaeducation.attendance.application.event.TeacherAbsentEvent;
import gn.lacaeducation.attendance.application.event.UndeclaredAttendanceEvent;
import gn.lacaeducation.audit.application.event.AccountantAnomalyDetectedEvent;
import gn.lacaeducation.billing.application.event.ClassTransferCreditSurplusEvent;
import gn.lacaeducation.billing.application.event.EcheanceDepasseeEvent;
import gn.lacaeducation.billing.application.event.InvoicePaymentAppliedEvent;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.*;
import gn.lacaeducation.grading.application.event.AssessmentScheduledEvent;
import gn.lacaeducation.grading.application.event.GradeModifiedEvent;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.iam.application.event.PasswordResetRequestedEvent;
import gn.lacaeducation.iam.application.event.UserAccountCreatedEvent;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.application.event.CashierSessionClosedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payroll.application.event.PayslipPaidEvent;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns domain events into outbound SMS/Email/WhatsApp communications to the student's parents (§11.1).
 * Each notification is system-generated (no human sender) and addressed by the parent's resolved
 * contact (phone/email), so the recipient may have no user account. When the tenant has configured a
 * {@link MessageTemplate} for the reason, its {@code {{variable}}} body is rendered; otherwise a
 * sensible French default is used.
 */
@Component @RequiredArgsConstructor @Slf4j
public class CommunicationEventListener {

    private final CommunicationUseCase communicationUseCase;
    private final MessageTemplateRepository templateRepository;
    private final StudentDataProvider studentDataProvider;
    private final PaymentDataProvider paymentDataProvider;
    private final PayerDirectory payerDirectory;
    private final AcademicsDataProvider academicsDataProvider;
    private final TenantInfoProvider tenantInfoProvider;
    private final gn.lacaeducation.iam.SchoolStaffDirectory schoolStaffDirectory;
    private final UserPublicInfoProvider userPublicInfoProvider;

    // Valeur Java par défaut IDENTIQUE à celle du SpEL ci-dessous (pas seulement une coïncidence) :
    // un test unitaire qui instancie ce listener sans contexte Spring (@InjectMocks) ne déclenche
    // jamais l'injection @Value — le champ resterait sinon null, et Map.of() (utilisé pour "lien")
    // lève une NullPointerException sur une valeur null, jamais juste un lien absent du SMS.
    @org.springframework.beans.factory.annotation.Value("${app.platform.reset-password-url:https://ecole.jeccamtech.com/reset-password}")
    private String resetPasswordUrl = "https://ecole.jeccamtech.com/reset-password";

    @org.springframework.beans.factory.annotation.Value("${app.platform.login-url:https://ecole.jeccamtech.com/login}")
    private String loginUrl = "https://ecole.jeccamtech.com/login";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH'h'mm");

    /**
     * Alerte par email, à chaque modification de note par un enseignant, l'administration de l'école, le
     * responsable pédagogique <em>et</em> le(s) parent(s) de l'élève, en précisant l'ancienne/nouvelle
     * valeur et le motif obligatoire (§7.2).
     */
    @TransactionalEventListener
    @Async
    public void onGradeModified(GradeModifiedEvent event) {
        String studentName = studentDataProvider.findStudent(event.studentId())
                .map(this::fullName).orElse("l'élève #" + event.studentId());
        String subject = "Modification d'une note";
        String body = "La note de " + studentName + " a été modifiée de " + event.oldValue()
                + " à " + event.newValue() + ". Motif : " + event.reason() + ".";

        // 1) Administration + responsable pédagogique (groupe interne).
        List<RecipientTarget> staffTargets = schoolStaffDirectory.findGradeChangeRecipients(event.tenantId()).stream()
                .filter(c -> c.email() != null && !c.email().isBlank())
                .map(c -> new RecipientTarget(null, c.email()))
                .toList();
        if (staffTargets.isEmpty()) {
            log.debug("Aucun destinataire (admin/responsable pédagogique) pour la modification de note {}",
                    event.gradeId());
        } else {
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), event.changedBy(),
                    CommunicationChannel.EMAIL, RecipientType.GROUP, subject, body,
                    CommunicationReason.GENERAL, staffTargets));
        }

        // 2) Parent(s) de l'élève par email.
        List<RecipientTarget> parentTargets = studentDataProvider.findParentsByStudent(event.studentId()).stream()
                .map(ParentPublicInfo::email)
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (parentTargets.isEmpty()) {
            log.debug("Aucun parent joignable par email pour la modification de note {}", event.gradeId());
        } else {
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), event.changedBy(),
                    CommunicationChannel.EMAIL, RecipientType.PARENT, subject, body,
                    CommunicationReason.GENERAL, parentTargets));
        }
    }

    /**
     * Notifie le parent par SMS à chaque pointage — présent à l'heure, en retard, ou absent — pas
     * uniquement les absences/retards comme avant : un scan de badge réussi doit confirmer l'arrivée
     * de l'enfant en temps réel, quelle que soit l'issue.
     */
    @TransactionalEventListener
    @Async
    public void onStudentAbsent(StudentAbsentEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String className = academicsDataProvider.findClass(event.classId()).map(c -> c.label()).orElse("N/A");
            String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
            String status = event.status();
            String time = event.checkInTime() != null ? event.checkInTime().format(TIME_FMT) : null;
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                    "classe", className, "date", date);
            // Corps raccourcis (§retour comptable, points 9/17) : tiennent en un seul segment SMS
            // même avec le préfixe "[Nom de l'école]" ajouté automatiquement à l'envoi.
            if ("LATE".equals(status)) {
                int minutes = event.lateMinutes() != null ? event.lateMinutes() : 0;
                String arrival = time != null ? " à " + time : "";
                String defaultBody = fullName(student) + " : retard de " + minutes + " min" + arrival
                        + " le " + date + " (classe " + className + ").";
                dispatch(event.tenantId(), CommunicationReason.LATE, "Retard signalé", defaultBody, vars, event.studentId());
            } else if ("PRESENT".equals(status)) {
                String arrival = time != null ? " à " + time : "";
                String defaultBody = fullName(student) + " est arrivé(e)" + arrival + " le " + date
                        + " (classe " + className + ").";
                dispatch(event.tenantId(), CommunicationReason.GENERAL, "Arrivée confirmée", defaultBody, vars, event.studentId());
            } else {
                String defaultBody = fullName(student) + " absent(e) le " + date + " (classe " + className + ").";
                dispatch(event.tenantId(), CommunicationReason.ABSENCE, "Absence signalée", defaultBody, vars, event.studentId());
            }
        });
    }

    /**
     * Notifie le parent au scan de sortie, avec la durée passée à l'école ce jour (§retour
     * fondateurs) — aucun lien de connexion ici (décision explicite : message quotidien, fréquent
     * par élève, le coût d'un 2e segment SMS n'est pas justifié, contrairement au paiement/bulletin).
     */
    @TransactionalEventListener
    @Async
    public void onStudentExit(StudentExitEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String className = academicsDataProvider.findClass(event.classId()).map(c -> c.label()).orElse("N/A");
            String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
            String time = event.checkOutTime() != null ? event.checkOutTime().format(TIME_FMT) : null;
            String duration = formatDuration(event.minutesAtSchool());
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                    "classe", className, "date", date);
            String defaultBody = fullName(student) + " est sorti(e)" + (time != null ? " à " + time : "")
                    + " le " + date + (duration != null ? " — " + duration + " à l'école" : "")
                    + " (classe " + className + ").";
            dispatch(event.tenantId(), CommunicationReason.GENERAL, "Sortie confirmée", defaultBody, vars, event.studentId());
        });
    }

    /** "1h30" / "45min" — null si la durée est inconnue (aucune entrée pointée ce jour-là). */
    private String formatDuration(int minutesAtSchool) {
        if (minutesAtSchool <= 0) return null;
        int hours = minutesAtSchool / 60;
        int minutes = minutesAtSchool % 60;
        if (hours == 0) return minutes + "min";
        return minutes == 0 ? hours + "h" : hours + "h" + minutes;
    }

    /**
     * Retard ou absence d'un enseignant (§retour fondateurs) : alerte à la fois la direction (pour
     * qu'elle puisse réagir — prévenir un remplaçant — sans surveiller l'emploi du temps en continu)
     * ET l'enseignant lui-même (message automatique par défaut ; le bouton manuel existant sur
     * l'emploi du temps reste disponible si la direction veut personnaliser le message). Jamais de
     * lien de connexion (même logique de coût que les SMS de pointage élève).
     */
    @TransactionalEventListener
    @Async
    public void onTeacherAbsent(TeacherAbsentEvent event) {
        String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
        boolean late = "LATE".equals(event.status());
        String adminContent;
        String teacherContent;
        if (late) {
            int minutes = event.lateMinutes() != null ? event.lateMinutes() : 0;
            String time = event.checkInTime() != null ? " à " + event.checkInTime().format(TIME_FMT) : "";
            adminContent = event.teacherName() + " est en retard (" + minutes + " min)" + time
                    + " le " + date + " — École " + schoolName(event.tenantId()) + ".";
            teacherContent = "Bonjour " + event.teacherName() + ", vous êtes en retard pour votre cours ("
                    + minutes + " min). Merci de nous contacter rapidement.";
        } else {
            adminContent = event.teacherName() + " est absent(e) le " + date
                    + " (aucun pointage détecté) — École " + schoolName(event.tenantId()) + ".";
            teacherContent = "Bonjour " + event.teacherName() + ", vous êtes signalé(e) absent(e) aujourd'hui. "
                    + "Merci de nous contacter.";
        }
        notifySchoolAdmins(event.tenantId(), late ? "Retard enseignant" : "Absence enseignant", adminContent);
        if (event.teacherPhone() != null && !event.teacherPhone().isBlank()) {
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, CommunicationChannel.SMS,
                    RecipientType.TEACHER, late ? "Retard" : "Absence", teacherContent,
                    late ? CommunicationReason.LATE : CommunicationReason.ABSENCE,
                    List.of(new RecipientTarget(null, event.teacherPhone()))));
        } else {
            log.info("Enseignant {} sans téléphone enregistré ; SMS de {} non envoyé à lui-même",
                    event.teacherId(), late ? "retard" : "absence");
        }
    }

    @TransactionalEventListener
    @Async
    public void onReportCardPublished(ReportCardPublishedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String period = academicsDataProvider.findPeriod(event.periodId()).map(p -> p.label()).orElse("N/A");
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()), "periode", period,
                    "lien", loginUrl);
            // Lien de connexion inclus (§retour fondateurs) : message peu fréquent (une fois par
            // période), contrairement aux SMS de pointage quotidien où le coût du 2e segment SMS
            // n'est pas justifié.
            String defaultBody = "Bulletin de " + fullName(student) + " (" + period + ") disponible. "
                    + "Consultez-le : " + loginUrl;
            dispatch(event.tenantId(), CommunicationReason.RESULT, "Bulletin disponible", defaultBody, vars, event.studentId());
        });
    }

    @TransactionalEventListener
    @Async
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        // Invoice-linked payments are handled by onInvoicePaymentApplied (which knows the remaining
        // balance). Here we only cover payments not tied to a single school-fee invoice — i.e. grouped
        // payments (plusieurs factures/mois réglés en une fois) — to avoid duplicate per-invoice SMS.
        if (event.invoiceId() != null) return;
        studentDataProvider.findStudent(event.studentId()).ifPresentOrElse(student ->
            paymentDataProvider.findPayment(event.paymentId()).ifPresentOrElse(payment -> {
                String amount = payment.amount() + " GNF";
                String coverage = briefCoverage(payment.coveredMonths());
                // Un paiement groupé ne déclenche jamais InvoicePaymentAppliedEvent par facture (voir
                // InvoicePaymentListener#onGroupedSettlement, notify=false) : le "reste à payer" doit
                // donc être calculé ici même, sinon ce SMS unique du règlement groupé (le seul envoyé
                // au parent dans ce cas) omettait complètement le solde restant (§retour comptable).
                String restLine = restLine(event.studentId());
                Map<String, String> vars = Map.of(
                        "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                        "montant", amount, "canal", payment.channel() == null ? "" : payment.channel(),
                        "reference", payment.reference() == null ? "" : payment.reference(),
                        "couverture", coverage, "reste_a_payer", restLine, "lien", loginUrl);
                // Contenu volontairement bref (§retour comptable) : montant payé + reste à payer,
                // sans détailler les mois couverts ni le canal. Lien ajouté UNIQUEMENT côté parent
                // (§retour fondateurs — paiement peu fréquent, contrairement au pointage quotidien) :
                // jamais dans paymentSmsBody lui-même, qui reste aussi le contenu du SMS comptable
                // (notifyFounder) — inutile de lui faire payer un 2e segment pour un lien qu'il connaît.
                String defaultBody = paymentSmsBody(fullName(student), amount, restLine) + " " + loginUrl;
                dispatch(event.tenantId(), CommunicationReason.GENERAL, "Paiement reçu", defaultBody, vars, event.studentId());
                notifyFounder(event.tenantId(), fullName(student), amount, restLine, event.collectedByUserId());
            }, () -> log.warn("PaymentSucceededEvent paymentId={} introuvable ; notification paiement ignorée", event.paymentId())),
            () -> log.warn("PaymentSucceededEvent studentId={} introuvable ; notification paiement ignorée", event.studentId()));
    }

    /**
     * Alerte le fondateur/SCHOOL_ADMIN par SMS dès qu'une session de caisse se clôture avec un écart
     * (§retour fondateurs, contrôle anti-fraude comptable) — jamais pour un écart nul (la clôture
     * reste tracée dans le journal d'audit, mais n'a rien d'alarmant). Le comptable concerné n'est
     * JAMAIS destinataire de cette alerte (contrairement à {@link #notifyFounder}) : s'il s'agit d'un
     * vol, le prévenir reviendrait à l'avertir qu'il a été repéré.
     */
    @TransactionalEventListener
    @Async
    public void onCashierSessionClosed(CashierSessionClosedEvent event) {
        if (event.discrepancy() == null || event.discrepancy().signum() == 0) return;
        String cashierName = userPublicInfoProvider.findUser(event.cashierId())
                .map(u -> ((u.firstName() != null ? u.firstName() : "") + " " + (u.lastName() != null ? u.lastName() : "")).trim())
                .filter(n -> !n.isBlank())
                .orElse("Comptable #" + event.cashierId());
        boolean missing = event.discrepancy().signum() < 0;
        String content = "École " + schoolName(event.tenantId()) + " : écart de caisse "
                + (missing ? "NÉGATIF (manquant)" : "positif (excédent)") + " de " + event.discrepancy().abs() + " GNF"
                + " à la clôture de " + cashierName + " (comptant " + event.physicalCount() + " GNF, système "
                + event.systemTotal() + " GNF). Vérifiez le rapprochement de caisse.";
        notifySchoolAdmins(event.tenantId(), "Écart de caisse", content);
    }

    /**
     * Retour comptable réel : un parent qui avait déjà réglé toute l'année dans l'ancienne classe se
     * retrouve, après un transfert vers une classe moins chère, avec un reliquat de crédit qu'aucune
     * facture ne peut plus absorber — ce surplus doit être remboursé au parent, jamais laissé filer
     * silencieusement dans les logs (voir ClassTransferInvoiceListener#publishSurplus).
     */
    @TransactionalEventListener
    @Async
    public void onClassTransferCreditSurplus(ClassTransferCreditSurplusEvent event) {
        String studentName = studentDataProvider.findStudent(event.studentId()).map(this::fullName)
                .orElse("Élève #" + event.studentId());
        String oldClassName = academicsDataProvider.findClass(event.oldClassId()).map(c -> c.label()).orElse("N/A");
        String newClassName = academicsDataProvider.findClass(event.newClassId()).map(c -> c.label()).orElse("N/A");
        String content = "École " + schoolName(event.tenantId()) + " : suite au transfert de " + studentName
                + " de " + oldClassName + " vers " + newClassName + ", un surplus de " + event.surplusAmount()
                + " GNF a déjà été réglé et ne peut plus être affecté à aucune facture. À REMBOURSER au parent.";
        notifySchoolAdmins(event.tenantId(), "Surplus à rembourser (transfert de classe)", content);
    }

    /**
     * Détecteur B2 (§retour fondateurs) : un encaissement guichet sans session de caisse ouverte pour
     * son collecteur n'a jamais été rapproché — alerte immédiate, jamais au comptable concerné.
     */
    @TransactionalEventListener
    @Async
    public void onCashCollectedWithoutOpenSession(gn.lacaeducation.payments.application.event.CashCollectedWithoutOpenSessionEvent event) {
        String cashierName = userPublicInfoProvider.findUser(event.cashierId())
                .map(u -> ((u.firstName() != null ? u.firstName() : "") + " " + (u.lastName() != null ? u.lastName() : "")).trim())
                .filter(n -> !n.isBlank())
                .orElse("Comptable #" + event.cashierId());
        String content = "École " + schoolName(event.tenantId()) + " : " + cashierName
                + " a encaissé " + event.amount() + " GNF au guichet SANS avoir ouvert de session de caisse. "
                + "Vérifiez cet encaissement.";
        notifySchoolAdmins(event.tenantId(), "Encaissement sans session de caisse", content);
    }

    /**
     * Détecteur B1 (§retour fondateurs) : un élève présent/en retard aujourd'hui sans inscription
     * active déclarée est le signal direct du vol décrit par les fondateurs (élève reçu et payé en
     * main à main, jamais déclaré dans le système). Alerte immédiate, le fautif potentiel (comptable
     * ou secrétaire ayant encaissé hors système) n'étant jamais identifiable à ce stade ne peut pas
     * être exclu par construction — mais ce n'est de toute façon pas lui qui saisit la présence
     * (l'enseignant, sans intérêt financier à mentir).
     */
    @TransactionalEventListener
    @Async
    public void onUndeclaredAttendance(UndeclaredAttendanceEvent event) {
        String studentName = studentDataProvider.findStudent(event.studentId())
                .map(this::fullName)
                .orElse("Élève #" + event.studentId());
        String content = "École " + schoolName(event.tenantId()) + " : " + studentName
                + " a été présent(e) en classe le " + event.date() + " SANS inscription déclarée dans le "
                + "système. Vérifiez si cet élève a été inscrit/encaissé hors système.";
        notifySchoolAdmins(event.tenantId(), "Présence sans inscription déclarée", content);
    }

    /**
     * Détecteur B3 (§retour fondateurs) : un comptable dépasse nettement la moyenne de ses pairs du
     * même établissement sur un indicateur (écarts de caisse, volume d'exonérations) — publié
     * uniquement à la création/réouverture de l'alerte (voir AccountantAnomalyAlertService), jamais
     * à chaque réévaluation hebdomadaire d'une alerte déjà active, pour éviter la lassitude aux
     * alertes. Le comptable concerné n'est JAMAIS destinataire.
     */
    @TransactionalEventListener
    @Async
    public void onAccountantAnomalyDetected(AccountantAnomalyDetectedEvent event) {
        String cashierName = userPublicInfoProvider.findUser(event.cashierId())
                .map(u -> ((u.firstName() != null ? u.firstName() : "") + " " + (u.lastName() != null ? u.lastName() : "")).trim())
                .filter(n -> !n.isBlank())
                .orElse("Comptable #" + event.cashierId());
        String label = event.type().equals("CASH_DISCREPANCY")
                ? "écarts de caisse cumulés (" + event.metricValue() + " GNF manquants, moyenne des pairs "
                        + event.peerAverage() + " GNF)"
                : "nombre d'exonérations accordées (" + event.metricValue() + ", moyenne des pairs "
                        + event.peerAverage() + ")";
        String content = "École " + schoolName(event.tenantId()) + " : " + cashierName
                + " se démarque nettement de ses collègues sur les " + label + " (30 derniers jours). "
                + "Vérifiez son activité récente.";
        notifySchoolAdmins(event.tenantId(), "Écart statistique comptable", content);
    }

    /**
     * Notify the parent of each payment applied to a school-fee invoice (§11.1, §retour comptable
     * point 1) : contenu bref — montant payé et solde encore dû (toutes factures confondues, pas
     * seulement celle qui vient d'être réglée : une mensualité soldée ne doit pas faire disparaître un
     * frais de cantine encore impayé du message), sans détailler le libellé du frais ni la classe.
     * Même contenu que le SMS envoyé au comptable ({@link #notifyFounder}) — les deux doivent être
     * identiques (§retour comptable).
     */
    @TransactionalEventListener
    @Async
    public void onInvoicePaymentApplied(InvoicePaymentAppliedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String amount = event.paidAmount() + " GNF";
            String paidLabel = payerDirectory.invoiceFeeInfo(event.invoiceId())
                    .map(info -> info.periodLabel() == null || info.periodLabel().isBlank()
                            ? info.feeTypeLabel() : info.feeTypeLabel() + " " + info.periodLabel())
                    .orElse(null);
            String className = payerDirectory.className(event.studentId()).orElse(null);
            String restLine = restLine(event.studentId());
            Map<String, String> vars = new HashMap<>(Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                    "montant", amount, "reste_a_payer", restLine,
                    "reference", event.reference() == null ? "" : event.reference()));
            vars.put("classe", className == null ? "" : className);
            vars.put("objet", paidLabel == null ? "" : paidLabel);
            String defaultBody = paymentSmsBody(fullName(student), amount, restLine);
            dispatch(event.tenantId(), CommunicationReason.GENERAL, "Paiement reçu", defaultBody, vars, event.studentId());
            notifyFounder(event.tenantId(), fullName(student), amount, restLine, event.collectedByUserId());
        });
    }

    /** Solde encore dû par l'élève, toutes factures confondues — juste le montant total (pas le détail
     * facture par facture, retiré du SMS pour rester bref, §retour comptable ; voir aussi
     * {@code OpenPdfReceiptGenerator#remainingLine}, même logique côté PDF où le détail reste utile). */
    private String restLine(Long studentId) {
        java.math.BigDecimal total = payerDirectory.unpaidInvoices(studentId).stream()
                .map(PayerDirectory.UnpaidInvoiceInfo::remaining)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return total.signum() <= 0
                ? "Aucun reste à payer."
                : "Reste à payer : " + total + " GNF.";
    }

    /** Contenu bref, commun au SMS parent et au SMS comptable (§retour comptable) : juste le montant
     * payé et le reste à payer, sans détailler le canal, la classe, le frais ou les mois couverts. */
    private String paymentSmsBody(String studentName, String amount, String restLine) {
        return "Paiement de " + amount + " reçu pour " + studentName + ".\n" + restLine;
    }

    /**
     * Relance le parent de l'élève dont la facture rattachée à l'échéance dépassée n'est pas soldée
     * (§8.2) — un événement par élève, déjà résolu côté {@code billing} (qui a accès à son propre
     * dépôt de factures ; `communication` n'est autorisé à dépendre que des événements de `billing`,
     * pas de son API de requête, voir ModularityTests). `event.tenantId()` est indispensable ici : ce
     * job tourne hors requête HTTP (planifié), donc sans {@link gn.lacaeducation.common.TenantContext}
     * déjà résolu par le filtre JWT — l'événement porte lui-même le tenant concerné plutôt que de
     * dépendre du contexte ambiant.
     */
    @TransactionalEventListener
    @Async
    public void onEcheanceDepassee(EcheanceDepasseeEvent event) {
        String amount = event.amount() + " GNF";
        String date = event.dueDate() != null ? event.dueDate().format(DATE_FMT) : "N/A";
        Map<String, String> vars = Map.of(
                "ecole", schoolName(event.tenantId()), "montant", amount, "date", date,
                "reference", event.invoiceReference() == null ? "" : event.invoiceReference());
        String defaultBody = "Rappel : échéance du " + date + " (" + amount + ") non réglée. Merci de régulariser.";
        dispatch(event.tenantId(), CommunicationReason.PAYMENT_DUE, "Échéance de paiement dépassée",
                defaultBody, vars, event.studentId());
    }

    /**
     * Notifie par SMS l'enseignant/personnel directement (pas via un parent) dès que sa fiche de paie
     * est marquée payée (§9, workflow Valider → Payer). Le téléphone/nom sont portés par l'événement
     * lui-même (figés sur la fiche à sa génération) plutôt que résolus ici, pour ne pas faire dépendre
     * {@code communication} de l'API de requête de {@code payroll} ni du référentiel {@code staff}.
     */
    @TransactionalEventListener
    @Async
    public void onPayslipPaid(PayslipPaidEvent event) {
        if (event.teacherPhone() == null || event.teacherPhone().isBlank()) {
            log.info("Fiche de paie {} payée sans téléphone enregistré ; notification SMS ignorée", event.payslipId());
            return;
        }
        String name = event.teacherName() != null ? event.teacherName() : "";
        String amount = event.amount() + " GNF";
        String period = event.periodLabel() != null ? event.periodLabel() : "";
        Map<String, String> vars = Map.of(
                "nom", name, "ecole", schoolName(event.tenantId()), "montant", amount, "periode", period);
        String defaultBody = "Bonjour " + name + ", salaire net de " + amount
                + (period.isBlank() ? "" : " (" + period + ")") + " payé.";
        Resolved r = resolve(event.tenantId(), CommunicationReason.SALARY_PAID, "Salaire payé", defaultBody, vars);
        communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, r.channel(),
                RecipientType.TEACHER, r.subject(), r.content(), CommunicationReason.SALARY_PAID,
                List.of(new RecipientTarget(null, event.teacherPhone()))));
        log.info("Notification SALARY_PAID envoyée pour la fiche de paie {} ({})", event.payslipId(), name);
    }

    /** Notify every (non-cancelled) student of the class on their own contact when an assessment is scheduled. */
    @TransactionalEventListener
    @Async
    public void onAssessmentScheduled(AssessmentScheduledEvent event) {
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(event.classId());
        if (students.isEmpty()) {
            log.debug("No student in class {}; skipping assessment notification", event.classId());
            return;
        }
        String className = academicsDataProvider.findClass(event.classId()).map(c -> c.label()).orElse("N/A");
        String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
        String label = (event.label() != null && !event.label().isBlank()) ? event.label() : "une évaluation";
        Map<String, String> vars = Map.of(
                "ecole", schoolName(event.tenantId()), "classe", className, "evaluation", label, "date", date);
        String defaultBody = "Évaluation (" + label + ") prévue le " + date + " pour la classe " + className + ".";

        Resolved r = resolve(event.tenantId(), CommunicationReason.EXAM, "Évaluation programmée", defaultBody, vars);
        List<RecipientTarget> targets = students.stream()
                .map(s -> studentDestination(s, r.channel()))
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (targets.isEmpty()) {
            log.debug("No reachable student in class {} on channel {}; skipping assessment notification",
                    event.classId(), r.channel());
            return;
        }
        communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, r.channel(),
                RecipientType.STUDENT, r.subject(), r.content(), CommunicationReason.EXAM, targets));
    }

    /**
     * Envoie ses identifiants de connexion à tout compte créé (personnel ou parent) — par SMS s'il a un
     * numéro (message bref, la connexion se fait par numéro de téléphone + mot de passe) ET par email
     * s'il a une adresse (message plus complet) : les deux canaux sont indépendants, l'un n'exclut plus
     * l'autre (avant : email envoyé seul dès qu'il existait, aucun SMS n'était alors envoyé — ce que
     * l'école recevait dépendait donc du hasard des champs renseignés). Pas de filtrage par accès
     * plateforme : ce n'est pas une notification au sujet d'un élève, mais l'accès du compte lui-même.
     */
    @TransactionalEventListener
    @Async
    public void onUserAccountCreated(UserAccountCreatedEvent event) {
        boolean hasEmail = event.email() != null && !event.email().isBlank();
        boolean hasPhone = event.phone() != null && !event.phone().isBlank();
        String fullName = ((event.firstName() != null ? event.firstName() : "") + " "
                + (event.lastName() != null ? event.lastName() : "")).trim();
        String password = event.plainPassword() == null ? "" : event.plainPassword();

        if (hasPhone) {
            // Bref : la connexion se fait par numéro de téléphone, pas par l'identifiant généré
            // (prenom.nom) — l'afficher n'aurait fait qu'induire en erreur sur la façon de se connecter.
            // Corps raccourci (§retour comptable, points 9/17) pour tenir en un seul segment SMS même
            // avec le préfixe école, malgré le lien de connexion difficilement compressible.
            Map<String, String> smsVars = Map.of(
                    "nom", fullName, "ecole", schoolName(event.tenantId()),
                    "mot_de_passe", password, "lien", loginUrl);
            String smsBody = "Connexion avec votre numéro, mot de passe " + password + " : " + loginUrl;
            Resolved r = resolve(event.tenantId(), CommunicationReason.ACCOUNT_CREATED,
                    "Votre compte a été créé", smsBody, smsVars);
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, CommunicationChannel.SMS,
                    RecipientType.GROUP, r.subject(), r.content(), CommunicationReason.ACCOUNT_CREATED,
                    List.of(new RecipientTarget(event.userId(), event.phone()))));
        }
        if (hasEmail) {
            Map<String, String> emailVars = Map.of(
                    "nom", fullName, "ecole", schoolName(event.tenantId()),
                    "identifiant", event.username() == null ? "" : event.username(),
                    "mot_de_passe", password, "lien", loginUrl);
            String emailBody = "Bienvenue " + fullName + ". Identifiant : " + event.username()
                    + " — Mot de passe : " + password + ". Connectez-vous pour le modifier : " + loginUrl;
            Resolved r = resolve(event.tenantId(), CommunicationReason.ACCOUNT_CREATED,
                    "Votre compte a été créé", emailBody, emailVars);
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, CommunicationChannel.EMAIL,
                    RecipientType.GROUP, r.subject(), r.content(), CommunicationReason.ACCOUNT_CREATED,
                    List.of(new RecipientTarget(event.userId(), event.email()))));
        }
    }

    /**
     * Envoie le lien de réinitialisation de mot de passe par email — sans lui, le jeton créé en base
     * à la demande « mot de passe oublié » n'était jamais communiqué à l'utilisateur (§2.1).
     */
    @TransactionalEventListener
    @Async
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        if (event.email() == null || event.email().isBlank()) return;
        String link = resetPasswordUrl + "?token=" + event.resetToken();
        Map<String, String> vars = Map.of("ecole", schoolName(event.tenantId()), "lien", link);
        String defaultBody = "Vous avez demandé la réinitialisation de votre mot de passe. "
                + "Suivez ce lien (valable 24h) pour en choisir un nouveau : " + link
                + "\nSi vous n'êtes pas à l'origine de cette demande, ignorez ce message.";
        Resolved r = resolve(event.tenantId(), CommunicationReason.PASSWORD_RESET,
                "Réinitialisation de votre mot de passe", defaultBody, vars);
        communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, CommunicationChannel.EMAIL,
                RecipientType.GROUP, r.subject(), r.content(), CommunicationReason.PASSWORD_RESET,
                List.of(new RecipientTarget(event.userId(), event.email()))));
    }

    /**
     * Envoie le code OTP de réinitialisation de mot de passe par SMS — alternative au lien email
     * ci-dessus pour un utilisateur sans email (la majorité : connexion par numéro de téléphone).
     */
    @TransactionalEventListener
    @Async
    public void onPasswordResetOtpRequested(gn.lacaeducation.iam.application.event.PasswordResetOtpRequestedEvent event) {
        if (event.phone() == null || event.phone().isBlank()) return;
        Map<String, String> vars = Map.of("ecole", schoolName(event.tenantId()), "code", event.code());
        String defaultBody = "Code de réinitialisation : " + event.code() + " (valable 10 min). "
                + "Ne le partagez avec personne.";
        Resolved r = resolve(event.tenantId(), CommunicationReason.PASSWORD_RESET,
                "Code de réinitialisation", defaultBody, vars);
        communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, CommunicationChannel.SMS,
                RecipientType.GROUP, r.subject(), r.content(), CommunicationReason.PASSWORD_RESET,
                List.of(new RecipientTarget(event.userId(), event.phone()))));
    }

    // ── dispatch ───────────────────────────────────────────────────────────────
    /** Resolved channel/subject/content after applying the tenant template (or French defaults). */
    private record Resolved(CommunicationChannel channel, String subject, String content) {}

    private Resolved resolve(Long tenantId, CommunicationReason reason, String defaultSubject,
                             String defaultBody, Map<String, String> vars) {
        String school = schoolName(tenantId);
        // Variable {{ecole}} disponible pour les modèles, sans toucher aux appelants.
        Map<String, String> allVars = new HashMap<>(vars == null ? Map.of() : vars);
        allVars.putIfAbsent("ecole", school);

        Optional<MessageTemplate> template = templateRepository.findByTenantIdAndReason(tenantId, reason);
        CommunicationChannel channel = template.map(MessageTemplate::getChannel).orElse(CommunicationChannel.SMS);
        if (channel == null) channel = CommunicationChannel.SMS;
        String subject = template.map(t -> MessageTemplate.render(t.getSubject(), allVars)).orElse(defaultSubject);
        String content = template.map(t -> MessageTemplate.render(t.getBody(), allVars)).orElse(defaultBody);

        // Le préfixe « [École] » sur les SMS est appliqué de façon centralisée par
        // CommunicationService#send (point d'envoi unique, garanti quel que soit l'appelant) — pas ici.
        return new Resolved(channel, subject, content);
    }

    private void dispatch(Long tenantId, CommunicationReason reason, String defaultSubject,
                          String defaultBody, Map<String, String> vars, Long studentId) {
        // Frais annexe optionnel (§billing) : seuls les élèves dont le parent a accepté l'accès à la
        // plateforme reçoivent les notifications automatiques (paiement, absence, retard, bulletin...)
        // — pas de facturation du frais plateforme, pas de notification, quel que soit le canal.
        if (!studentDataProvider.hasPlatformAccessOptIn(studentId)) {
            log.info("Élève {} sans accès plateforme accepté ; notification {} ignorée", studentId, reason);
            return;
        }
        Resolved r = resolve(tenantId, reason, defaultSubject, defaultBody, vars);
        List<RecipientTarget> targets = studentDataProvider.findParentsByStudent(studentId).stream()
                .map(parent -> destinationFor(parent, r.channel()))
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (targets.isEmpty()) {
            log.info("No reachable parent for student {} on channel {}; skipping {} notification", studentId, r.channel(), reason);
            return;
        }
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, r.channel(),
                RecipientType.PARENT, r.subject(), r.content(), reason, targets));
        log.info("Notification {} envoyée à {} destinataire(s) pour l'élève {} via {}",
                reason, targets.size(), studentId, r.channel());
    }

    /**
     * Numéro(s) à alerter pour les sujets « fondateur » (paiement reçu, écarts de caisse,
     * exonérations suspectes, présence sans inscription, retard/absence enseignant…) — §retour
     * fondateurs : « l'alerte n'arrive pas toujours sur le numéro du fondateur ». Si l'école a
     * renseigné un numéro de fondateur dédié ({@code Tenant#founderPhone}, Paramètres de l'école),
     * on alerte UNIQUEMENT ce numéro. Sinon, on retombe sur tous les comptes SCHOOL_ADMIN
     * (comportement historique, nécessaire tant que ce numéro n'est pas configuré).
     */
    private List<String> founderAlertPhones(Long tenantId) {
        String founderPhone = tenantInfoProvider.findTenantInfo(tenantId)
                .map(TenantPublicInfo::founderPhone)
                .filter(p -> p != null && !p.isBlank())
                .orElse(null);
        if (founderPhone != null) {
            return List.of(founderPhone);
        }
        return schoolStaffDirectory.findSchoolAdminContacts(tenantId).stream()
                .map(gn.lacaeducation.iam.SchoolStaffDirectory.SchoolAdminContact::phone)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Alerte par SMS le numéro du fondateur (voir {@link #founderAlertPhones}) dès qu'un paiement est
     * reçu, en plus de la notification au parent. Alerte aussi le comptable/caissier qui a
     * physiquement encaissé ({@code collectedByUserId}, canal CASH uniquement), pas seulement le
     * fondateur — son numéro peut être différent (point 2, retour comptable). Un même numéro n'est
     * jamais alerté deux fois (ex. le fondateur encaisse lui-même). Contenu identique au SMS du
     * parent ({@link #paymentSmsBody}) : le comptable/fondateur doit voir le même montant payé et le
     * même reste à payer, pas un message différent (§retour comptable).
     */
    private void notifyFounder(Long tenantId, String studentName, String amount, String restLine, Long collectedByUserId) {
        java.util.LinkedHashMap<String, String> byPhone = new java.util.LinkedHashMap<>();
        founderAlertPhones(tenantId).forEach(phone -> byPhone.putIfAbsent(phone, phone));
        if (collectedByUserId != null) {
            userPublicInfoProvider.findUser(collectedByUserId).ifPresent(u -> {
                if (u.phone() != null && !u.phone().isBlank()) byPhone.putIfAbsent(u.phone(), u.phone());
            });
        }
        if (byPhone.isEmpty()) {
            log.debug("Aucun numéro fondateur/SCHOOL_ADMIN/comptable joignable par SMS pour le tenant {}; alerte fondateur ignorée", tenantId);
            return;
        }
        List<RecipientTarget> targets = byPhone.values().stream().map(phone -> new RecipientTarget(null, phone)).toList();
        String content = paymentSmsBody(studentName, amount, restLine);
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, CommunicationChannel.SMS,
                RecipientType.GROUP, "Paiement reçu", content, CommunicationReason.GENERAL, targets));
    }

    /**
     * SMS au numéro du fondateur (voir {@link #founderAlertPhones} ; jamais le comptable/l'auteur de
     * l'action concernée) — réservé aux alertes de sécurité (§retour fondateurs). Contrairement à
     * {@link #notifyFounder}, ne reçoit aucun identifiant d'utilisateur à inclure en plus.
     */
    private void notifySchoolAdmins(Long tenantId, String subject, String content) {
        List<String> phones = founderAlertPhones(tenantId);
        if (phones.isEmpty()) {
            log.debug("Aucun numéro fondateur/SCHOOL_ADMIN joignable par SMS pour le tenant {} ; alerte sécurité ignorée", tenantId);
            return;
        }
        List<RecipientTarget> targets = phones.stream().map(phone -> new RecipientTarget(null, phone)).toList();
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, CommunicationChannel.SMS,
                RecipientType.GROUP, subject, content, CommunicationReason.SECURITY_ALERT, targets));
    }

    private String destinationFor(ParentPublicInfo parent, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? parent.email() : parent.phone();
    }

    private String studentDestination(StudentPublicInfo student, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? student.email() : student.phone();
    }

    private String schoolName(Long tenantId) {
        if (tenantId == null) return "L'établissement";
        return tenantInfoProvider.findTenantInfo(tenantId).map(TenantPublicInfo::schoolName).orElse("L'établissement");
    }

    private String fullName(StudentPublicInfo s) {
        return ((s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "")).trim();
    }

    /** Version courte de {@code coveredMonths} (ex. "Octobre, Novembre, Décembre, Janvier" → "Octobre,
     * Novembre, …") pour ne jamais faire gonfler le SMS à plusieurs segments sur une grosse fratrie/
     * tranche multi-mois. Vide (jamais null) si rien à afficher. */
    private static String briefCoverage(String coveredMonths) {
        if (coveredMonths == null || coveredMonths.isBlank()) return "";
        String trimmed = coveredMonths.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 37) + "…" : trimmed;
    }
}
