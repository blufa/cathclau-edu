package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.calendar.CalendarDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.lessons.TeacherTeachingProvider;
import gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase;
import gn.lacaeducation.payroll.application.port.out.PayslipLineRepository;
import gn.lacaeducation.payroll.application.port.out.PayslipPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryRuleRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock PayslipRepository payslipRepo;
    @Mock PayslipLineRepository lineRepo;
    @Mock SalaryStructureRepository structureRepo;
    @Mock SalaryRuleRepository ruleRepo;
    @Mock PayslipPdfGenerator pdfGenerator;
    @Mock TeacherCompensationRepository compRepo;
    @Mock gn.lacaeducation.payroll.application.port.out.TeacherVacationPayRepository vacationPayRepo;
    @Mock TeacherTeachingProvider teachingProvider;
    @Mock TeacherWorkProvider workProvider;
    @Mock SalaryFeeDeductionUseCase salaryFeeDeductionUseCase;
    @Mock SalaryAdvanceUseCase salaryAdvanceUseCase;
    @Mock AcademicsDataProvider academics;
    @Mock CalendarDataProvider calendar;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock gn.lacaeducation.iam.UserPublicInfoProvider userPublicInfoProvider;
    @Mock gn.lacaeducation.staff.TeacherPublicInfoProvider teacherPublicInfoProvider;
    @Mock gn.lacaeducation.staff.EmployeePublicInfoProvider employeePublicInfoProvider;

    @InjectMocks PayrollService payrollService;

    @BeforeEach
    void defaultNoStaffBenefitRemainder() {
        // La plupart des tests n'atteignent jamais ce calcul (ex. teacherPayrollMass) — lenient pour
        // éviter une erreur "unnecessary stubbing" sur ces tests-là.
        lenient().when(salaryFeeDeductionUseCase.computeStaffBenefitRemainder(any(), any(), any(), any(), any()))
                .thenReturn(new SalaryFeeDeductionUseCase.StaffBenefitRemainderDeduction(BigDecimal.ZERO, 0));
        // Idem : aucun jour férié par défaut, sauf test dédié à computeMonthlyPay_slotCoveredByHoliday*.
        lenient().when(calendar.paidHolidayDates(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void teacherPayrollMass_sumsFixedSalaryDirectlyAndHourlyViaTheoreticalPay() {
        TeacherCompensation fixedTeacher = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("1500000")).build();
        TeacherCompensation hourlyTeacher = TeacherCompensation.builder().teacherId(2L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("20000")).hourlyRateExam(new BigDecimal("25000"))
                .fixedSalary(BigDecimal.ZERO).build();

        when(compRepo.findByTenantId(9L)).thenReturn(List.of(fixedTeacher, hourlyTeacher));
        // Le calcul théorique de l'enseignant HOURLY interroge compRepo.findByTeacherId puis
        // l'emploi du temps (vide ici -> aucune heure théorique, gross = 0).
        when(compRepo.findByTeacherId(2L)).thenReturn(Optional.of(hourlyTeacher));
        when(academics.findScheduleByTeacher(2L)).thenReturn(List.of());

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.teacherCount()).isEqualTo(2);
        assertThat(mass.totalHourlyRateNormal()).isEqualByComparingTo("20000");
        assertThat(mass.totalHourlyRateExam()).isEqualByComparingTo("25000");
        // FIXED teacher contributes his fixed salary directly ; HOURLY teacher has no scheduled
        // hours in this test, so his theoretical gross pay is 0.
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("1500000");
    }

    @Test
    void teacherPayrollMass_maternelleDistinguishedFromPrimaire() {
        // Retour fondateur : "il faut afficher aussi le salaire des enseignants de niveau maternelle"
        // sur l'état financier du mois — Maternelle ne doit plus être fondue dans "Primaire".
        TeacherCompensation maternelleTeacher = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("700000")).build();
        when(compRepo.findByTenantId(9L)).thenReturn(List.of(maternelleTeacher));
        // §retour utilisateur : le cycle se résout depuis les "Niveaux enseignés" (checklist
        // déclarative sur la fiche enseignant), jamais depuis ses classes/matières assignées.
        when(teacherPublicInfoProvider.findAssignedLevelIds(1L)).thenReturn(List.of(300L));
        when(academics.findLevelCycle(300L)).thenReturn(Optional.of("MATERNELLE"));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.maternelleMonthlyPayroll()).isEqualByComparingTo("700000");
        assertThat(mass.primaryMonthlyPayroll()).isEqualByComparingTo("0");
        assertThat(mass.secondaryMonthlyPayroll()).isEqualByComparingTo("0");
    }

    @Test
    void resolveTeacherCycles_returnsOneBucketPerRequestedTeacher() {
        // Retour utilisateur : générer la paie "par niveau" (colonne Cycle sur Rémunération & heures,
        // sélection rapide Maternelle/Primaire/Secondaire à la génération en lot).
        when(teacherPublicInfoProvider.findAssignedLevelIds(1L)).thenReturn(List.of(300L));
        when(academics.findLevelCycle(300L)).thenReturn(Optional.of("MATERNELLE"));
        when(teacherPublicInfoProvider.findAssignedLevelIds(2L)).thenReturn(List.of(100L));
        when(academics.findLevelCycle(100L)).thenReturn(Optional.of("PRIMAIRE"));
        when(teacherPublicInfoProvider.findAssignedLevelIds(3L)).thenReturn(List.of());

        java.util.Map<Long, String> cycles = payrollService.resolveTeacherCycles(List.of(1L, 2L, 3L));

        assertThat(cycles).containsEntry(1L, "MATERNELLE").containsEntry(2L, "PRIMAIRE").containsEntry(3L, "SECONDAIRE");
    }

    @Test
    void resolveTeacherCycles_declaresBothPrimaryAndSecondaryLevels_prefersPrimary() {
        // Incident réel (§retour comptable : "quand je filtre par cycle rien ne vient", puis "base-toi
        // sur le niveau enseigné, pas sur les matières") : un enseignant déclarant à la fois un niveau
        // de primaire ET un niveau de secondaire dans sa checklist "Niveaux enseignés" doit ressortir
        // Primaire (présence simple, jamais un décompte majoritaire) — jamais invisible du filtre
        // "Primaire" du modal "Générer les fiches de paie en lot".
        when(teacherPublicInfoProvider.findAssignedLevelIds(1L)).thenReturn(List.of(100L, 200L));
        when(academics.findLevelCycle(100L)).thenReturn(Optional.of("PRIMAIRE"));
        when(academics.findLevelCycle(200L)).thenReturn(Optional.of("COLLEGE"));

        java.util.Map<Long, String> cycles = payrollService.resolveTeacherCycles(List.of(1L));

        assertThat(cycles).containsEntry(1L, "PRIMAIRE");
    }

    @Test
    void teacherPayrollMass_splitsByPrimaryVsSecondaryCycle() {
        // Retour fondateur (état financier du mois) : un enseignant Primaire (classe principale CP1,
        // cycle PRIMAIRE) et un enseignant Secondaire (matière assignée en 6ème, cycle COLLEGE).
        TeacherCompensation primaryTeacher = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("1000000")).build();
        TeacherCompensation secondaryTeacher = TeacherCompensation.builder().teacherId(2L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("1200000")).build();
        when(compRepo.findByTenantId(9L)).thenReturn(List.of(primaryTeacher, secondaryTeacher));
        when(teacherPublicInfoProvider.findAssignedLevelIds(1L)).thenReturn(List.of(100L));
        when(academics.findLevelCycle(100L)).thenReturn(Optional.of("PRIMAIRE"));
        when(teacherPublicInfoProvider.findAssignedLevelIds(2L)).thenReturn(List.of(200L));
        when(academics.findLevelCycle(200L)).thenReturn(Optional.of("COLLEGE"));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.primaryMonthlyPayroll()).isEqualByComparingTo("1000000");
        assertThat(mass.secondaryMonthlyPayroll()).isEqualByComparingTo("1200000");
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("2200000");
    }

    @Test
    void teacherPayrollMass_teacherWithNoResolvedClass_defaultsToSecondary() {
        // Même convention de repli que grading.domain.CycleGroup#fromSchoolCycle pour un cycle non
        // renseigné : par défaut Secondaire, jamais Primaire par supposition.
        TeacherCompensation unassigned = TeacherCompensation.builder().teacherId(3L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("500000")).build();
        when(compRepo.findByTenantId(9L)).thenReturn(List.of(unassigned));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.primaryMonthlyPayroll()).isEqualByComparingTo("0");
        assertThat(mass.secondaryMonthlyPayroll()).isEqualByComparingTo("500000");
    }

    @Test
    void teacherPayrollMass_withValidTeacherIdsFilter_ignoresOrphanedCompensation() {
        // §retour utilisateur (Vision Fondateur en production) : un enseignant du primaire à salaire
        // fixe, supprimé, continuait de gonfler la masse salariale théorique — sa fiche
        // TeacherCompensation reste orpheline en base, jamais nettoyée à la suppression.
        TeacherCompensation deletedTeacher = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("1500000")).build();
        TeacherCompensation activeTeacher = TeacherCompensation.builder().teacherId(2L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("900000")).build();
        when(compRepo.findByTenantId(9L)).thenReturn(List.of(deletedTeacher, activeTeacher));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3, java.util.Set.of(2L));

        assertThat(mass.teacherCount()).isEqualTo(1);
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("900000");
    }

    @Test
    void teacherPayrollMass_withValidTeacherIdsFilter_countsNewTeacherWithoutCompensationToo() {
        // §retour utilisateur : "peu importe primaire/secondaire, salaire configuré ou non, il doit
        // être affiché sur la vision fondateur" — le nombre d'enseignants reflète le référentiel réel
        // (3 enseignants), même si un seul a une fiche TeacherCompensation.
        TeacherCompensation configured = TeacherCompensation.builder().teacherId(1L)
                .payMode(PayMode.FIXED).hourlyRateNormal(BigDecimal.ZERO).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(new BigDecimal("900000")).build();
        when(compRepo.findByTenantId(9L)).thenReturn(List.of(configured));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3, java.util.Set.of(1L, 2L, 3L));

        assertThat(mass.teacherCount()).isEqualTo(3);
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("900000");
    }

    @Test
    void teacherPayrollMass_teacherWithoutCompensationButWithRemunerationOnHisOwnRecord_stillCounted() {
        // §retour utilisateur : "même si il n'est pas affecté à une matière et une classe, le cumul de
        // tout le salaire doit venir" — un enseignant jamais passé par la génération d'une fiche de
        // paie (donc sans TeacherCompensation) mais dont le salaire mensuel a été saisi à la création
        // (Teacher#remunerationType/Rate) doit déjà compter dans la masse salariale, même SANS aucune
        // niveau enseigné déclaré (resolveCycleBucket tombe donc sur Secondaire par défaut, mais le
        // montant ne doit jamais disparaître pour autant).
        when(compRepo.findByTenantId(9L)).thenReturn(List.of());
        when(teacherPublicInfoProvider.listTeachers(9L)).thenReturn(List.of(
                new gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo(
                        4L, "Nouveau", "Moniteur", "T-4", null, null, null, null, "MENSUEL", new BigDecimal("450000"))));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3, java.util.Set.of(4L));

        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("450000");
        assertThat(mass.secondaryMonthlyPayroll()).isEqualByComparingTo("450000");
    }

    @Test
    void teacherPayrollMass_teacherWithoutCompensationAndHourlyType_contributesZero() {
        // HORAIRE sans TeacherCompensation : aucun taux normal/examen séparé connu, donc pas de montant
        // sûr à ajouter (contrairement à MENSUEL/FORFAIT, un seul taux global) — reste à 0 jusqu'à ce
        // qu'une vraie TeacherCompensation soit configurée, pas de régression sur ce cas.
        when(compRepo.findByTenantId(9L)).thenReturn(List.of());
        when(teacherPublicInfoProvider.listTeachers(9L)).thenReturn(List.of(
                new gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo(
                        5L, "Nouveau", "Horaire", "T-5", null, null, null, null, "HORAIRE", new BigDecimal("5000"))));

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3, java.util.Set.of(5L));

        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("0");
    }

    @Test
    void teacherPayrollMass_noTeachers_isZero() {
        when(compRepo.findByTenantId(9L)).thenReturn(List.of());

        PayrollService.TeacherPayrollMass mass = payrollService.teacherPayrollMass(9L, 2026, 3);

        assertThat(mass.teacherCount()).isZero();
        assertThat(mass.totalHourlyRateNormal()).isEqualByComparingTo("0");
        assertThat(mass.monthlyPayroll()).isEqualByComparingTo("0");
    }

    /** La durée choisie séance par séance dans le modal « Réaliser » (théorique OU réellement pointée)
     * est la seule source de vérité pour la paie — le retard/départ anticipé cumulé du pointage reste
     * calculé et affiché sur la fiche de paie (information), mais n'est plus déduit une seconde fois du
     * temps réalisé : sans quoi choisir la durée RÉELLE pointée (qui exclut déjà le retard) pénalisait
     * l'enseignant deux fois pour le même retard. */
    @Test
    void computeMonthlyPay_doesNotDeductLateOrEarlyFromRealizedMinutes() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of(20L, 120));
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9))
                .thenReturn(Map.of(20L, new TeacherWorkProvider.ClassLateEarly(351, 40)));
        when(academics.findClass(20L)).thenReturn(Optional.empty());

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        // 120 min réalisées (choisies dans le modal) = 2h x 10 000 GNF, sans déduction du retard/départ
        // anticipé.
        assertThat(pay.effectiveMinutes()).isEqualTo(120);
        assertThat(pay.teachingPay()).isEqualByComparingTo("20000");
        assertThat(pay.grossPay()).isEqualByComparingTo("20000");
        // Retard/départ anticipé restent réellement calculés et exposés (affichage informatif seulement).
        assertThat(pay.lateMinutes()).isEqualTo(351);
        assertThat(pay.earlyMinutes()).isEqualTo(40);
    }

    /** Un événement école (kermesse, match...) qui couvre un créneau récurrent doit garder ces heures
     * payées, même si aucune séance n'a été marquée réalisée pour cette date (les élèves étant rentrés
     * à la maison) — voir eventCoveredMinutesByClass, jamais retirées des heures. */
    @Test
    void computeMonthlyPay_slotCoveredBySchoolEvent_countsAsRealizedEvenWithoutLessonEntry() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of()); // rien réalisé au cahier de textes
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findClass(20L)).thenReturn(Optional.empty());
        // Créneau récurrent : samedi 8h-10h (120 min), classe 20 — ex. le professeur d'EPS.
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "10:00", null)));
        // Événement école couvrant le samedi 5 septembre 2026 (journée entière, pas d'heure précisée) —
        // la direction a explicitement crédité (payé) cet enseignant pour cet événement.
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of(
                new gn.lacaeducation.academics.SchoolEventPublicInfo(1L, "Kermesse",
                        java.time.LocalDate.of(2026, 9, 5), java.time.LocalDate.of(2026, 9, 5), null, null)));
        when(academics.isTeacherCreditedForSchoolEvent(1L, 5L)).thenReturn(true);

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        assertThat(pay.effectiveMinutes()).isEqualTo(120);
        assertThat(pay.grossPay()).isEqualByComparingTo("20000");
    }

    /** Régression : un enseignant tout juste ajouté à l'emploi du temps ne doit PAS être crédité
     * rétroactivement pour un événement école déjà configuré ce mois-ci tant que la direction ne l'a
     * pas explicitement choisi — sinon "Heures réalisées" apparaît rempli sans qu'aucun cours n'ait
     * jamais été donné (bug réel vécu : professeur nouvellement créé et placé dans l'emploi du temps). */
    @Test
    void computeMonthlyPay_slotCoveredByOtherEvent_teacherNotCredited_doesNotCount() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of());
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "10:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of(
                new gn.lacaeducation.academics.SchoolEventPublicInfo(1L, "Kermesse",
                        java.time.LocalDate.of(2026, 9, 5), java.time.LocalDate.of(2026, 9, 5), null, null)));
        when(academics.isTeacherCreditedForSchoolEvent(1L, 5L)).thenReturn(false);

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        assertThat(pay.effectiveMinutes()).isEqualTo(0);
        assertThat(pay.grossPay()).isEqualByComparingTo("0");
    }

    /** Un jour férié (chômé et payé par l'État — calendrier académique §calendar,
     * {@code AcademicEventType.HOLIDAY}) reste TOUJOURS payé sur les heures touchées, automatiquement,
     * sans passer par le mécanisme de crédit des SchoolEvent (réservé aux événements décidés par la
     * direction) — corrige le cas réel d'un enseignant tout juste créé/placé dans l'emploi du temps qui
     * ne doit PAS être payé pour un jour férié qu'il n'a jamais couvert (voir aussi le test précédent
     * pour les événements école). */
    @Test
    void computeMonthlyPay_slotCoveredByPaidHoliday_alwaysCountsWithoutCreditCheck() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of());
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "10:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of());
        // Fête de l'indépendance déclarée jour férié dans le calendrier académique (§calendar).
        when(calendar.paidHolidayDates(eq(1L), any(), any())).thenReturn(
                List.of(java.time.LocalDate.of(2026, 9, 5)));

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        assertThat(pay.effectiveMinutes()).isEqualTo(120);
        assertThat(pay.grossPay()).isEqualByComparingTo("20000");
        org.mockito.Mockito.verify(academics, org.mockito.Mockito.never()).isTeacherCreditedForSchoolEvent(any(), any());
    }

    @Test
    void computeMonthlyPay_slotOutsideEventTimeWindow_doesNotCount() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of());
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        // Créneau samedi 8h-10h, mais l'événement ne couvre que 14h-18h ce jour-là : pas de chevauchement.
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "10:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of(
                new gn.lacaeducation.academics.SchoolEventPublicInfo(1L, "Kermesse",
                        java.time.LocalDate.of(2026, 9, 5), java.time.LocalDate.of(2026, 9, 5),
                        java.time.LocalTime.of(14, 0), java.time.LocalTime.of(18, 0))));

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        assertThat(pay.effectiveMinutes()).isEqualTo(0);
        assertThat(pay.grossPay()).isEqualByComparingTo("0");
    }

    /** Enseignant "exceptionnel" (§5.6, ex. professeur d'EPS) qui rassemble plusieurs classes au même
     * horaire (samedi 8h-12h, classes 20/21/22) : ses heures sont FIXÉES par le créneau, jamais
     * multipliées par le nombre de classes couvertes en même temps — 4h (240 min) payées une seule
     * fois, même si les 3 classes ont chacune une séance marquée réalisée ce jour-là. */
    @Test
    void computeMonthlyPay_concurrentClassesSameSlot_countsHoursOnceNotPerClass() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findClass(20L)).thenReturn(Optional.empty());
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "12:00", null),
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(2L, 21L, 3L, 5L, null, "SAMEDI", "08:00", "12:00", null),
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(3L, 22L, 3L, 5L, null, "SAMEDI", "08:00", "12:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of());
        // Les 3 classes ont chacune une séance marquée réalisée le samedi 5 septembre 2026 (240 min).
        when(teachingProvider.realizedMinutesByClassAndDate(eq(5L), any(), any())).thenReturn(Map.of(
                java.time.LocalDate.of(2026, 9, 5), Map.of(20L, 240, 21L, 240, 22L, 240)));

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        // 4h payées une seule fois (240 min), jamais 12h (720 min = 3 x 240) : pas de comptage par classe.
        assertThat(pay.effectiveMinutes()).isEqualTo(240);
        assertThat(pay.grossPay()).isEqualByComparingTo("40000");
    }

    /** Même principe qu'au-dessus, mais côté événement école (§5.6 + événement) : le bloc concurrent
     * couvert par un événement (élèves rentrés à la maison) reste payé une seule fois. */
    @Test
    void computeMonthlyPay_schoolEventCoversConcurrentBlock_countsBlockOnceNotPerClass() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(teachingProvider.realizedMinutesByClassAndDate(eq(5L), any(), any())).thenReturn(Map.of());
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findClass(20L)).thenReturn(Optional.empty());
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "SAMEDI", "08:00", "12:00", null),
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(2L, 21L, 3L, 5L, null, "SAMEDI", "08:00", "12:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of(
                new gn.lacaeducation.academics.SchoolEventPublicInfo(1L, "Kermesse",
                        java.time.LocalDate.of(2026, 9, 5), java.time.LocalDate.of(2026, 9, 5), null, null)));
        when(academics.isTeacherCreditedForSchoolEvent(1L, 5L)).thenReturn(true);

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        assertThat(pay.effectiveMinutes()).isEqualTo(240);
        assertThat(pay.grossPay()).isEqualByComparingTo("40000");
    }

    /** Régression : deux créneaux simplement CONSÉCUTIFS (ex. 8h-9h puis 9h-10h, deux classes
     * différentes à la suite — cas normal très courant, pas un enseignant exceptionnel) ne doivent
     * jamais être traités comme un bloc "concurrent". Si le fusionnement se déclenchait à tort sur des
     * créneaux qui se touchent sans se chevaucher, les heures réalisées des deux classes seraient
     * dédupliquées et sous-payées, y compris pour un enseignant normal sans aucun chevauchement réel. */
    @Test
    void computeMonthlyPay_backToBackSlotsDifferentClasses_notTreatedAsConcurrent_sumsNormally() {
        TeacherCompensation comp = TeacherCompensation.builder().teacherId(5L).tenantId(1L)
                .payMode(PayMode.HOURLY).hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                .fixedSalary(BigDecimal.ZERO).build();
        when(compRepo.findByTeacherId(5L)).thenReturn(Optional.of(comp));
        when(workProvider.monthlyLateEarlyByClass(5L, 2026, 9)).thenReturn(Map.of());
        when(academics.findClass(20L)).thenReturn(Optional.empty());
        when(academics.findClass(21L)).thenReturn(Optional.empty());
        // Lundi 8h-9h classe 20, puis 9h-10h classe 21 : consécutifs, jamais simultanés.
        when(academics.findScheduleByTeacher(5L)).thenReturn(List.of(
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(1L, 20L, 3L, 5L, null, "LUNDI", "08:00", "09:00", null),
                new gn.lacaeducation.academics.ScheduleSlotPublicInfo(2L, 21L, 3L, 5L, null, "LUNDI", "09:00", "10:00", null)));
        when(academics.findSchoolEvents(eq(1L), any(), any())).thenReturn(List.of());
        when(teachingProvider.realizedMinutesByClass(eq(5L), any(), any())).thenReturn(Map.of(20L, 60, 21L, 60));

        PayrollService.MonthlyPay pay = payrollService.computeMonthlyPay(5L, 2026, 9);

        // 1h + 1h = 2h, chacune payée séparément — pas de déduplication entre créneaux consécutifs.
        assertThat(pay.effectiveMinutes()).isEqualTo(120);
        assertThat(pay.grossPay()).isEqualByComparingTo("20000");
    }

    /** Fiche combinée : personnel de direction (salaire fixe) qui enseigne aussi (heures réalisées) —
     * une seule fiche, salaire de base + une ligne EARNING pour les heures d'enseignement. */
    @Test
    void generateCombinedPayslip_addsBaseSalaryPlusTeachingHoursLine() {
        Long employeeId = 5L;
        Long linkedTeacherId = 42L;
        BigDecimal baseSalary = new BigDecimal("2000000");

        when(salaryAdvanceUseCase.applyDeduction(anyLong(), eq(employeeId), any(), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(BigDecimal.ZERO, null));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(100L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(100L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(100L)).thenAnswer(i -> lines);

        // Enseignant lié : 2h réalisées (120 min), aucun retard/départ anticipé, tarif normal 10 000.
        when(teachingProvider.realizedMinutesByClass(eq(linkedTeacherId), any(), any())).thenReturn(Map.of(1L, 120));
        when(workProvider.monthlyLateEarlyByClass(eq(linkedTeacherId), eq(2026), eq(3))).thenReturn(Map.of());
        when(compRepo.findByTeacherId(linkedTeacherId)).thenReturn(Optional.of(
                TeacherCompensation.builder().teacherId(linkedTeacherId).payMode(PayMode.HOURLY)
                        .hourlyRateNormal(new BigDecimal("10000")).hourlyRateExam(BigDecimal.ZERO)
                        .fixedSalary(BigDecimal.ZERO).build()));

        Payslip result = payrollService.generateCombinedPayslip(9L, employeeId, "Mariam Bah",
                EmployeeType.DIRECTION, baseSalary, linkedTeacherId, 2026, 3, null);

        assertThat(result.getEmployeeType()).isEqualTo(EmployeeType.DIRECTION);
        assertThat(result.getTeacherId()).isEqualTo(employeeId);
        assertThat(result.getBaseSalary()).isEqualByComparingTo(baseSalary);
        // 2h × 10 000 = 20 000 de paie d'enseignement, ajoutée en plus du salaire de base fixe.
        assertThat(result.getGrossSalary()).isEqualByComparingTo(baseSalary.add(new BigDecimal("20000.00")));
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLineType()).isEqualTo(PayslipLineType.EARNING);
    }

    @Test
    void generateCombinedPayslip_withoutLinkedTeacher_throws() {
        assertThatThrownBy(() -> payrollService.generateCombinedPayslip(9L, 5L, "Mariam Bah",
                EmployeeType.DIRECTION, new BigDecimal("2000000"), null, 2026, 3, null))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }

    /** Fait remonter la paie dans la trésorerie (§payroll ↔ accounting), jamais dans le budget annuel
     * voté (masse salariale toujours puisée dans les recettes de l'école, pas dans ce budget) : au
     * paiement d'une fiche, l'événement porte le net à virer. */
    @Test
    void markPaid_publishesPayslipPaidEventWithNetSalary() {
        Payslip validated = Payslip.builder().id(1L).tenantId(9L).teacherId(7L).employeeType(EmployeeType.TEACHER)
                .status(gn.lacaeducation.payroll.domain.PayslipStatus.VALIDATED)
                .netSalary(new BigDecimal("850000")).teacherName("Mamadou Diallo").teacherPhone("622000000")
                .periodLabel("Mars 2026").build();
        when(payslipRepo.findById(1L)).thenReturn(java.util.Optional.of(validated));
        when(payslipRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        payrollService.markPaid(1L, "BANK_TRANSFER", 42L);

        org.mockito.Mockito.verify(eventPublisher).publishEvent(
                new gn.lacaeducation.payroll.application.event.PayslipPaidEvent(9L, 1L, new BigDecimal("850000"),
                        "Mamadou Diallo", "622000000", "Mars 2026"));
        // Répercute la retenue (solde réel avantage personnel) sur les factures des enfants du
        // salarié, maintenant que la fiche est réellement payée (§payroll ↔ billing).
        org.mockito.Mockito.verify(salaryFeeDeductionUseCase).applyStaffBenefitRemainderAsInvoicePayment(9L, EmployeeType.TEACHER, 7L, null, "Mars 2026");
    }

    /** Empêche un double-clic/rejeu de repayer une fiche déjà payée — sans ce garde-fou, la retenue
     * scolarité serait répercutée deux fois sur les factures des enfants (voir markPaid). */
    @Test
    void markPaid_alreadyPaid_throwsAndDoesNotReapplyDeduction() {
        Payslip paid = Payslip.builder().id(1L).tenantId(9L).teacherId(7L)
                .status(gn.lacaeducation.payroll.domain.PayslipStatus.PAID)
                .netSalary(new BigDecimal("850000")).build();
        when(payslipRepo.findById(1L)).thenReturn(java.util.Optional.of(paid));

        assertThatThrownBy(() -> payrollService.markPaid(1L, "BANK_TRANSFER", 42L))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
        org.mockito.Mockito.verify(salaryFeeDeductionUseCase, org.mockito.Mockito.never())
                .applyDeductionAsInvoicePayment(any(), any(), any());
        org.mockito.Mockito.verify(salaryFeeDeductionUseCase, org.mockito.Mockito.never())
                .applyStaffBenefitRemainderAsInvoicePayment(any(), any(), any(), any(), any());
        org.mockito.Mockito.verify(payslipRepo, org.mockito.Mockito.never()).save(any());
    }

    /** Empêche un doublon silencieux (double-clic sur « Générer en lot »/« Générer une fiche ») : une
     * fiche existe déjà pour ce salarié sur cette période exacte (même type d'employé). */
    @Test
    void generate_duplicateForSameTeacherAndPeriod_throwsConflict() {
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, 7L, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(true);

        assertThatThrownBy(() -> payrollService.generate(new PayrollService.GenerateCommand(
                9L, null, 7L, "M. Diallo", "Mars 2026",
                null, null, new BigDecimal("300000"), null, null,
                null, null, null, null, null, null, null)))
                .isInstanceOf(gn.lacaeducation.common.ConflictException.class);
    }

    /** Le remboursement d'une avance sur salaire en cours (§payroll) est appliqué automatiquement à
     * chaque génération de fiche, comme la retenue de frais scolaires — voir
     * {@code SalaryAdvanceService#applyDeduction}, ici simulée par le mock. */
    @Test
    void generate_appliesSalaryAdvanceDeductionAsLine() {
        Long teacherId = 7L;
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, teacherId, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(false);
        when(salaryAdvanceUseCase.applyDeduction(eq(9L), eq(teacherId), eq(EmployeeType.TEACHER), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(new BigDecimal("50000"), "Avance sur salaire"));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(200L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(200L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(200L)).thenAnswer(i -> lines);

        Payslip result = payrollService.generate(new PayrollService.GenerateCommand(
                9L, EmployeeType.TEACHER, teacherId, "M. Diallo", "Mars 2026",
                null, null, new BigDecimal("300000"), null, null,
                null, null, null, null, null, null, null));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLineType()).isEqualTo(PayslipLineType.DEDUCTION);
        assertThat(lines.get(0).getLabel()).isEqualTo("Avance sur salaire");
        assertThat(lines.get(0).getAmount()).isEqualByComparingTo("50000");
        assertThat(result.getNetSalary()).isEqualByComparingTo("250000");
    }

    /** Double profil (§retour utilisateur urgence production, "il ne prend pas l'avance sur salaire") :
     * avance accordée sous l'identité Employee (ex. Direction), fiche générée sous l'identité Teacher
     * (ex. depuis la page "Paie du mois") — doit quand même être retenue via la fiche Employee liée
     * ({@code Employee#linkedTeacherId}). */
    @Test
    void generate_teacherWithLinkedEmployeeAdvance_appliesDeductionViaLinkedIdentity() {
        Long teacherId = 7L;
        Long employeeId = 70L;
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, teacherId, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(false);
        // Rien sous l'identité Teacher : l'avance a été accordée sous l'identité Personnel (Direction).
        when(salaryAdvanceUseCase.applyDeduction(eq(9L), eq(teacherId), eq(EmployeeType.TEACHER), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(BigDecimal.ZERO, null));
        when(employeePublicInfoProvider.findByLinkedTeacherId(9L, teacherId)).thenReturn(Optional.of(
                new gn.lacaeducation.staff.EmployeePublicInfoProvider.EmployeeInfo(
                        employeeId, "M. Diallo", "DIRECTION", null, true, null, null, null, null, teacherId)));
        when(salaryAdvanceUseCase.applyDeduction(eq(9L), eq(employeeId), eq(EmployeeType.DIRECTION), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(new BigDecimal("50000"), "Avance sur salaire"));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(201L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(201L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(201L)).thenAnswer(i -> lines);

        Payslip result = payrollService.generate(new PayrollService.GenerateCommand(
                9L, EmployeeType.TEACHER, teacherId, "M. Diallo", "Mars 2026",
                null, null, new BigDecimal("300000"), null, null,
                null, null, null, null, null, null, null));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLabel()).isEqualTo("Avance sur salaire");
        assertThat(lines.get(0).getAmount()).isEqualByComparingTo("50000");
        assertThat(result.getNetSalary()).isEqualByComparingTo("250000");
    }

    /** Net déjà à zéro/négatif à ce stade (ex. enseignant à l'heure, peu d'heures + grosses retenues) :
     * l'avance active existe mais ne peut pas être prélevée sans pousser le net plus bas — doit rester
     * visible sur la fiche (note), jamais silencieuse (§retour utilisateur : "il ne prend pas l'avance",
     * confusion alors qu'elle est juste reportée faute de net suffisant). */
    @Test
    void generate_netAlreadyExhausted_notesOutstandingAdvanceInsteadOfSilentlySkipping() {
        Long teacherId = 7L;
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, teacherId, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(false);
        when(salaryAdvanceUseCase.applyDeduction(eq(9L), eq(teacherId), eq(EmployeeType.TEACHER), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(BigDecimal.ZERO, null));
        when(employeePublicInfoProvider.findByLinkedTeacherId(9L, teacherId)).thenReturn(Optional.empty());
        when(salaryAdvanceUseCase.findByTeacherId(9L, teacherId, EmployeeType.TEACHER)).thenReturn(List.of(
                SalaryAdvance.builder().id(1L).status(SalaryAdvanceStatus.ACTIVE)
                        .remainingBalance(new BigDecimal("150000")).build()));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(202L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(202L)).thenAnswer(i -> Optional.of(saved.get()));
        when(lineRepo.findByPayslipId(202L)).thenReturn(List.of());

        Payslip result = payrollService.generate(new PayrollService.GenerateCommand(
                9L, EmployeeType.TEACHER, teacherId, "M. Diallo", "Mars 2026",
                null, null, BigDecimal.ZERO, null, null,
                null, null, null, null, null, null, null));

        assertThat(result.getNotes()).contains("150 000").contains("NON prélevée");
    }

    /** Reste non couvert par un avantage personnel PARTIEL (§billing) : ajouté comme ligne de retenue
     * distincte de la déduction scolarité à montant fixe (§payroll), voir applyStaffBenefitRemainderDeduction. */
    @Test
    void generate_appliesStaffBenefitRemainderDeductionAsLine() {
        Long teacherId = 7L;
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(9L, teacherId, "Mars 2026", EmployeeType.TEACHER))
                .thenReturn(false);
        when(salaryFeeDeductionUseCase.computeStaffBenefitRemainder(9L, EmployeeType.TEACHER, teacherId, null, "Mars 2026"))
                .thenReturn(new SalaryFeeDeductionUseCase.StaffBenefitRemainderDeduction(new BigDecimal("20000"), 1));
        when(salaryAdvanceUseCase.applyDeduction(eq(9L), eq(teacherId), eq(EmployeeType.TEACHER), any()))
                .thenReturn(new SalaryAdvanceUseCase.AdvanceDeductionResult(BigDecimal.ZERO, null));

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(201L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(201L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(201L)).thenAnswer(i -> lines);

        Payslip result = payrollService.generate(new PayrollService.GenerateCommand(
                9L, EmployeeType.TEACHER, teacherId, "M. Diallo", "Mars 2026",
                null, null, new BigDecimal("300000"), null, null,
                null, null, null, null, null, null, null));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLineType()).isEqualTo(PayslipLineType.DEDUCTION);
        assertThat(lines.get(0).getLabel()).isEqualTo("Reste avantage personnel (1 enfant(s))");
        assertThat(lines.get(0).getAmount()).isEqualByComparingTo("20000");
        assertThat(result.getNetSalary()).isEqualByComparingTo("280000");
    }

    /** §retour comptable : "j'ai retiré deux factures, j'ai rechargé mais le montant global de la
     * retenue n'a pas changé" — la ligne "Reste avantage personnel" ne se met jamais à jour toute seule
     * quand une facture est retirée/réincluse après la génération (§billing, modal "Détail de la
     * retenue") ; refreshStaffBenefitDeductionLine la recalcule explicitement. */
    @Test
    void refreshStaffBenefitDeductionLine_replacesStaleLineWithFreshAmount() {
        Payslip p = Payslip.builder().id(50L).tenantId(9L).employeeType(EmployeeType.TEACHER)
                .teacherId(7L).periodLabel("Mars 2026").status(gn.lacaeducation.payroll.domain.PayslipStatus.DRAFT)
                .baseSalary(new BigDecimal("300000")).build();
        when(payslipRepo.findById(50L)).thenReturn(Optional.of(p));
        when(payslipRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PayslipLine stale = PayslipLine.builder().id(900L).payslipId(50L)
                .label("Reste avantage personnel (2 enfant(s))").lineType(PayslipLineType.DEDUCTION)
                .amount(new BigDecimal("60000")).build();
        List<PayslipLine> lines = new ArrayList<>(List.of(stale));
        when(lineRepo.findByPayslipId(50L)).thenAnswer(i -> new ArrayList<>(lines));
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        org.mockito.Mockito.doAnswer(i -> {
            Long id = i.getArgument(0);
            lines.removeIf(l -> id.equals(l.getId()));
            return null;
        }).when(lineRepo).deleteById(any());

        // Une facture a été retirée depuis : il ne reste plus que 20 000 pour 1 seul enfant (au lieu de
        // 60 000 pour 2) — simule le nouvel état réel après le retrait côté billing.
        when(salaryFeeDeductionUseCase.computeStaffBenefitRemainder(9L, EmployeeType.TEACHER, 7L, null, "Mars 2026"))
                .thenReturn(new SalaryFeeDeductionUseCase.StaffBenefitRemainderDeduction(new BigDecimal("20000"), 1));

        Payslip result = payrollService.refreshStaffBenefitDeductionLine(50L);

        verify(lineRepo).deleteById(900L);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLabel()).isEqualTo("Reste avantage personnel (1 enfant(s))");
        assertThat(lines.get(0).getAmount()).isEqualByComparingTo("20000");
        assertThat(result.getNetSalary()).isEqualByComparingTo("280000");
    }

    @Test
    void refreshStaffBenefitDeductionLine_paidPayslip_throwsAndNeverModifies() {
        Payslip p = Payslip.builder().id(51L).tenantId(9L).employeeType(EmployeeType.TEACHER)
                .teacherId(7L).periodLabel("Mars 2026").status(gn.lacaeducation.payroll.domain.PayslipStatus.PAID)
                .baseSalary(new BigDecimal("300000")).build();
        when(payslipRepo.findById(51L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> payrollService.refreshStaffBenefitDeductionLine(51L))
                .isInstanceOf(BusinessRuleException.class);
        verify(lineRepo, never()).deleteById(any());
    }

    /**
     * Régression réelle (§retour comptable, tenant "Groupe Scolaire Privé Notre Mère" — bug reproduit
     * en base : personnel administratif salaire 1 000 000 GNF, 2 enfants avec avantage personnel 30 %
     * chacun scolarisés — la mensualité de chaque enfant est éclatée en 9 factures dès l'inscription
     * (opt-in mensuel). Avant le correctif, la retenue sommait le solde de TOUTES ces factures
     * (9 mois × 2 enfants) sur une seule fiche de paie, donnant -1 345 000 GNF de net (plus que le
     * salaire lui-même) — et cette inflation empêchait aussi toute avance sur salaire de se déduire
     * (net disponible déjà négatif). Ce test vérifie que : 1) seul le solde du mois de paie précis est
     * retenu (315 000 = 157 500 × 2 enfants pour novembre, jamais les 9 mois), et 2) une avance sur
     * salaire active se déduit correctement PAR-DESSUS, sur le net réellement disponible.
     */
    @Test
    void generate_staffBenefitRemainderScopedToPeriod_leavesRoomForSalaryAdvanceDeduction() {
        Long employeeId = 8L;
        java.time.LocalDate periodStart = null; // comme en base réelle : period_start/period_end vides
        when(payslipRepo.existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(
                4L, employeeId, "Novembre 2026", EmployeeType.ADMINISTRATIVE)).thenReturn(false);
        // Reflète les données réelles : 2 enfants, 157 500 GNF encore dus chacun pour NOVEMBRE
        // uniquement (le mois d'octobre, déjà réglé au guichet, ne doit rien ajouter).
        when(salaryFeeDeductionUseCase.computeStaffBenefitRemainder(
                4L, EmployeeType.ADMINISTRATIVE, employeeId, periodStart, "Novembre 2026"))
                .thenReturn(new SalaryFeeDeductionUseCase.StaffBenefitRemainderDeduction(new BigDecimal("315000"), 2));
        // Avance active de 200 000 GNF encore due — l'appel réel calcule min(soldeAvance, netDisponible)
        // : on vérifie ici que le net disponible transmis reflète bien la retenue avantage personnel
        // déjà scopée au mois (685 000), jamais un net déjà négatif par la somme de 9 mois.
        when(salaryAdvanceUseCase.applyDeduction(eq(4L), eq(employeeId), eq(EmployeeType.ADMINISTRATIVE), any()))
                .thenAnswer(inv -> {
                    BigDecimal availableNet = inv.getArgument(3);
                    BigDecimal advanceRemaining = new BigDecimal("200000");
                    BigDecimal deducted = advanceRemaining.min(availableNet);
                    return new SalaryAdvanceUseCase.AdvanceDeductionResult(deducted, "Avance sur salaire");
                });

        AtomicReference<Payslip> saved = new AtomicReference<>();
        when(payslipRepo.save(any())).thenAnswer(i -> {
            Payslip p = i.getArgument(0);
            if (p.getId() == null) p.setId(300L);
            saved.set(p);
            return p;
        });
        when(payslipRepo.findById(300L)).thenAnswer(i -> Optional.of(saved.get()));

        List<PayslipLine> lines = new ArrayList<>();
        when(lineRepo.save(any())).thenAnswer(i -> {
            PayslipLine l = i.getArgument(0);
            lines.add(l);
            return l;
        });
        when(lineRepo.findByPayslipId(300L)).thenAnswer(i -> lines);

        Payslip result = payrollService.generate(new PayrollService.GenerateCommand(
                4L, EmployeeType.ADMINISTRATIVE, employeeId, "Salif Soumah", "Novembre 2026",
                periodStart, null, new BigDecimal("1000000"), null, null,
                null, null, null, null, null, null, null));

        var staffBenefitLine = lines.stream().filter(l -> l.getLabel().startsWith("Reste avantage personnel")).findFirst().orElseThrow();
        assertThat(staffBenefitLine.getAmount()).isEqualByComparingTo("315000");
        var advanceLine = lines.stream().filter(l -> "Avance sur salaire".equals(l.getLabel())).findFirst().orElseThrow();
        assertThat(advanceLine.getAmount()).isEqualByComparingTo("200000");
        // 1 000 000 (base) - 315 000 (avantage personnel, UN mois) - 200 000 (avance) = 485 000.
        assertThat(result.getNetSalary()).isEqualByComparingTo("485000");
    }

    @Test
    void pdfBytes_notYetPaid_fallsBackToGeneratingUserForAccountantName() {
        Payslip p = Payslip.builder().id(500L).tenantId(10L).employeeType(EmployeeType.ADMINISTRATIVE)
                .teacherId(7L).teacherName("Mariama Diallo").status(gn.lacaeducation.payroll.domain.PayslipStatus.VALIDATED)
                .paidByUserId(null).build();
        when(payslipRepo.findById(500L)).thenReturn(Optional.of(p));
        when(lineRepo.findByPayslipId(500L)).thenReturn(List.of());
        when(userPublicInfoProvider.findUser(42L)).thenReturn(Optional.of(
                new gn.lacaeducation.iam.UserPublicInfoProvider.UserPublicInfo(42L, "Alpha", "Camara", null)));

        payrollService.pdfBytes(500L, null, null, 42L);

        org.mockito.ArgumentCaptor<PayslipPdfGenerator.Presentation> captor =
                org.mockito.ArgumentCaptor.forClass(PayslipPdfGenerator.Presentation.class);
        org.mockito.Mockito.verify(pdfGenerator).generate(eq(p), any(), any(), captor.capture());
        assertThat(captor.getValue().accountantName()).isEqualTo("Alpha Camara");
    }

    @Test
    void pdfBytes_alreadyPaid_prefersPayerOverGeneratingUser() {
        Payslip p = Payslip.builder().id(501L).tenantId(10L).employeeType(EmployeeType.ADMINISTRATIVE)
                .teacherId(7L).teacherName("Mariama Diallo").status(gn.lacaeducation.payroll.domain.PayslipStatus.PAID)
                .paidByUserId(99L).build();
        when(payslipRepo.findById(501L)).thenReturn(Optional.of(p));
        when(lineRepo.findByPayslipId(501L)).thenReturn(List.of());
        when(userPublicInfoProvider.findUser(99L)).thenReturn(Optional.of(
                new gn.lacaeducation.iam.UserPublicInfoProvider.UserPublicInfo(99L, "Comptable", "Titulaire", null)));

        // generatedByUserId (42L) ne doit JAMAIS être résolu : la fiche est déjà payée par 99L.
        payrollService.pdfBytes(501L, null, null, 42L);

        org.mockito.ArgumentCaptor<PayslipPdfGenerator.Presentation> captor =
                org.mockito.ArgumentCaptor.forClass(PayslipPdfGenerator.Presentation.class);
        org.mockito.Mockito.verify(pdfGenerator).generate(eq(p), any(), any(), captor.capture());
        assertThat(captor.getValue().accountantName()).isEqualTo("Comptable Titulaire");
        org.mockito.Mockito.verify(userPublicInfoProvider, org.mockito.Mockito.never()).findUser(42L);
    }

}
