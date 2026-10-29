package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase;
import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.ApplicableExemption;
import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.StaffCoverage;
import gn.lacaeducation.billing.application.port.out.StaffChildBenefitPolicyRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffChildBenefitServiceTest {

    @Mock StaffChildBenefitPolicyRepository repo;
    @Mock ParentAccessProvider parentAccessProvider;
    @Mock TeacherPublicInfoProvider teacherProvider;
    @Mock EmployeePublicInfoProvider employeeProvider;
    @Mock FamilyProvider familyProvider;
    @Mock ExemptionUseCase exemptionUseCase;
    @InjectMocks StaffChildBenefitService service;

    private StaffChildBenefitPolicy policyFirstChildFullyCovered() {
        return StaffChildBenefitPolicy.builder().id(1L).label("Personnel — 1er enfant")
                .coveredChildrenCount(1).coveragePercent(new BigDecimal("100")).feeTypes(List.of(FeeType.MONTHLY)).active(true).build();
    }

    private void staffParentWithTwoChildren() {
        when(parentAccessProvider.findParentUserIdsByStudent(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.of(9L)); // parent = employé
        when(repo.findByTenantId(1L)).thenReturn(List.of(policyFirstChildFullyCovered()));
        lenient().when(familyProvider.findSiblings(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of(
                new ChildInfo(1L, LocalDate.of(2010, 1, 1)),   // aîné
                new ChildInfo(2L, LocalDate.of(2015, 1, 1))));  // cadet
    }

    @Test
    void computeCoverage_firstChildOfStaff_fullyCovered() {
        staffParentWithTwoChildren();

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("100");
        assertThat(c.policyLabel()).contains("Personnel");
    }

    @Test
    void computeCoverage_secondChildOfStaff_notCovered() {
        staffParentWithTwoChildren();

        StaffCoverage c = service.computeCoverage(1L, 2L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("0");
    }

    @Test
    void computeCoverage_notStaffChild_returnsZero() {
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.empty()); // pas enseignant
        when(employeeProvider.findEmployeeIdByUserId(50L)).thenReturn(Optional.empty()); // pas personnel non plus

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("0");
    }

    @Test
    void computeCoverageBatch_returnsOnlyStudentsWithNonZeroCoverage() {
        staffParentWithTwoChildren();

        List<gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.StudentCoverage> result =
                service.computeCoverageBatch(1L, List.of(1L, 2L), FeeType.MONTHLY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(1L);
        assertThat(result.get(0).coveragePercent()).isEqualByComparingTo("100");
    }

    @Test
    void computeCoverage_childOfNonTeachingStaff_isCoveredJustLikeChildOfTeacher() {
        // Le parent n'est PAS enseignant (teacherProvider vide), mais son compte est lié à une fiche
        // Employee (personnel administratif/direction/soutien) — doit être reconnu comme enfant du
        // personnel au même titre qu'un enfant d'enseignant (régression du bug corrigé : avant, seul
        // le référentiel Enseignant était consulté).
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.empty());
        when(employeeProvider.findEmployeeIdByUserId(50L)).thenReturn(Optional.of(12L));
        when(repo.findByTenantId(1L)).thenReturn(List.of(policyFirstChildFullyCovered()));
        when(familyProvider.findSiblings(1L)).thenReturn(List.of(new ChildInfo(1L, LocalDate.of(2012, 1, 1))));

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("100");
        assertThat(c.policyLabel()).contains("Personnel");
    }

    @Test
    void computeCoverage_staffChildViaDirectLinkWithoutAccount_isCovered() {
        // Personnel SANS compte de connexion (ex. pas de smartphone Android) : lié directement sur la
        // fiche Parent (voir ParentAccessProvider#findDirectStaffLinksByStudent), aucun userId partagé.
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of());
        when(parentAccessProvider.findDirectStaffLinksByStudent(1L))
                .thenReturn(List.of(new ParentAccessProvider.DirectStaffLink("TEACHER", 9L)));
        when(repo.findByTenantId(1L)).thenReturn(List.of(policyFirstChildFullyCovered()));
        when(familyProvider.findSiblings(1L)).thenReturn(List.of(new ChildInfo(1L, LocalDate.of(2012, 1, 1))));

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("100");
    }

    @Test
    void computeCoverage_staffChildOptedOutOfAutoDeduction_isNotTreatedAsStaffChild() {
        // Retour comptable : un enseignant qui paie réellement lui-même les frais de son enfant (réglage
        // excludeFromAutoDeduction sur sa fiche Parent) ne doit plus compter comme "enfant du personnel",
        // ni pour ce reporting ni pour le tag avantage personnel sur facture (InvoiceService).
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.of(9L));
        when(parentAccessProvider.hasOptedOutOfStaffAutoDeduction(1L)).thenReturn(true);

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("0");
    }

    @Test
    void findUncoveredStaffChildIds_staffChildWithNoExemption_isReturned() {
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.of(9L));
        when(exemptionUseCase.computeApplicable(1L, FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(BigDecimal.ZERO, null, false, BigDecimal.ZERO, null));

        List<Long> result = service.findUncoveredStaffChildIds(1L, List.of(1L));

        assertThat(result).containsExactly(1L);
    }

    @Test
    void findUncoveredStaffChildIds_staffChildWithExistingExemption_isExcluded() {
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.of(9L));
        when(exemptionUseCase.computeApplicable(1L, FeeType.MONTHLY))
                .thenReturn(new ApplicableExemption(new BigDecimal("100"), "Direction", true, BigDecimal.ZERO, null));

        List<Long> result = service.findUncoveredStaffChildIds(1L, List.of(1L));

        assertThat(result).isEmpty();
    }

    @Test
    void findUncoveredStaffChildIds_notStaffChild_isExcluded() {
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.empty());
        when(employeeProvider.findEmployeeIdByUserId(50L)).thenReturn(Optional.empty());

        List<Long> result = service.findUncoveredStaffChildIds(1L, List.of(1L));

        assertThat(result).isEmpty();
    }
}
