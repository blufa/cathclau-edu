package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.billing.application.event.StaffChildDeductionExcludedEvent;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.ParentAccessProvider.DirectStaffLink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffChildDeductionRefreshListenerTest {

    @Mock ParentAccessProvider parentAccessProvider;
    @Mock TeacherPublicInfoProvider teacherProvider;
    @Mock EmployeePublicInfoProvider employeeProvider;
    @Mock PayslipRepository payslipRepo;
    @Mock PayrollService payrollService;
    @InjectMocks StaffChildDeductionRefreshListener listener;

    @Test
    void onStaffChildDeductionExcluded_directTeacherLink_refreshesOnlyNonPaidTeacherPayslips() {
        when(parentAccessProvider.findParentUserIdsByStudent(11L)).thenReturn(List.of());
        when(parentAccessProvider.findDirectStaffLinksByStudent(11L)).thenReturn(List.of(new DirectStaffLink("TEACHER", 9L)));
        Payslip draft = Payslip.builder().id(100L).employeeType(EmployeeType.TEACHER).status(PayslipStatus.DRAFT).build();
        Payslip paid = Payslip.builder().id(101L).employeeType(EmployeeType.TEACHER).status(PayslipStatus.PAID).build();
        Payslip otherStaffType = Payslip.builder().id(102L).employeeType(EmployeeType.ADMINISTRATIVE).status(PayslipStatus.DRAFT).build();
        when(payslipRepo.findByTenantIdAndTeacherId(4L, 9L)).thenReturn(List.of(draft, paid, otherStaffType));

        listener.onStaffChildDeductionExcluded(new StaffChildDeductionExcludedEvent(4L, 11L, true));

        verify(payrollService).refreshStaffBenefitDeductionLine(100L);
        verify(payrollService, never()).refreshStaffBenefitDeductionLine(101L);
        verify(payrollService, never()).refreshStaffBenefitDeductionLine(102L);
    }

    @Test
    void onStaffChildDeductionExcluded_sharedAccountEmployee_resolvesNonTeacherPayslip() {
        when(parentAccessProvider.findParentUserIdsByStudent(11L)).thenReturn(List.of(77L));
        when(teacherProvider.findTeacherIdByUserId(77L)).thenReturn(Optional.empty());
        when(employeeProvider.findEmployeeIdByUserId(77L)).thenReturn(Optional.of(5L));
        when(parentAccessProvider.findDirectStaffLinksByStudent(11L)).thenReturn(List.of());
        Payslip employeeDraft = Payslip.builder().id(200L).employeeType(EmployeeType.DIRECTION).status(PayslipStatus.VALIDATED).build();
        Payslip teacherPayslipSameId = Payslip.builder().id(201L).employeeType(EmployeeType.TEACHER).status(PayslipStatus.DRAFT).build();
        when(payslipRepo.findByTenantIdAndTeacherId(4L, 5L)).thenReturn(List.of(employeeDraft, teacherPayslipSameId));

        listener.onStaffChildDeductionExcluded(new StaffChildDeductionExcludedEvent(4L, 11L, false));

        verify(payrollService).refreshStaffBenefitDeductionLine(200L);
        // Même id référentiel partagé entre Teacher et Employee (voir javadoc PayslipRepository) : ne
        // doit jamais rafraîchir la fiche enseignant par erreur.
        verify(payrollService, never()).refreshStaffBenefitDeductionLine(201L);
    }

    @Test
    void onStaffChildDeductionExcluded_refreshThrows_otherPayslipsStillProcessed() {
        when(parentAccessProvider.findParentUserIdsByStudent(11L)).thenReturn(List.of());
        when(parentAccessProvider.findDirectStaffLinksByStudent(11L)).thenReturn(List.of(new DirectStaffLink("TEACHER", 9L)));
        Payslip locked = Payslip.builder().id(100L).employeeType(EmployeeType.TEACHER).status(PayslipStatus.DRAFT).build();
        Payslip other = Payslip.builder().id(101L).employeeType(EmployeeType.TEACHER).status(PayslipStatus.DRAFT).build();
        when(payslipRepo.findByTenantIdAndTeacherId(4L, 9L)).thenReturn(List.of(locked, other));
        when(payrollService.refreshStaffBenefitDeductionLine(100L)).thenThrow(new RuntimeException("fiche déjà payée entre-temps"));

        listener.onStaffChildDeductionExcluded(new StaffChildDeductionExcludedEvent(4L, 11L, true));

        verify(payrollService).refreshStaffBenefitDeductionLine(101L);
    }
}
