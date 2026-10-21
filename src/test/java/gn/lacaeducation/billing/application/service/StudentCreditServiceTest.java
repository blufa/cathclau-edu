package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.StudentCreditRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCreditServiceTest {

    @Mock StudentCreditRepository repo;
    @Mock InvoiceUseCase invoiceUseCase;

    private StudentCreditService service() {
        return new StudentCreditService(repo, invoiceUseCase);
    }

    private StudentCredit lot(Long id, Long studentId, String amount, String remaining) {
        return StudentCredit.builder().id(id).tenantId(10L).studentId(studentId)
                .amount(new BigDecimal(amount)).remainingAmount(new BigDecimal(remaining))
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service().create(10L, 7L, BigDecimal.ZERO, null, 1L, "x"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void availableBalance_sumsRemainingAcrossLots() {
        when(repo.findByStudentId(7L)).thenReturn(List.of(
                lot(1L, 7L, "100", "40"), lot(2L, 7L, "50", "50"), lot(3L, 7L, "30", "0")));

        assertThat(service().availableBalance(7L)).isEqualByComparingTo("90");
    }

    @Test
    void applyToInvoice_rejectsInvoiceBelongingToAnotherStudent() {
        Invoice other = Invoice.builder().id(5L).tenantId(10L).studentId(99L)
                .totalAmount(new BigDecimal("100")).remainingBalance(new BigDecimal("100"))
                .status(InvoiceStatus.UNPAID).build();
        when(invoiceUseCase.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service().applyToInvoice(7L, 5L, new BigDecimal("50"), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("n'appartient pas");
        verify(invoiceUseCase, never()).applyCredit(any(), any(), any());
    }

    @Test
    void applyToInvoice_rejectsWhenInsufficientCredit() {
        Invoice inv = Invoice.builder().id(5L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100")).remainingBalance(new BigDecimal("100"))
                .status(InvoiceStatus.UNPAID).build();
        when(invoiceUseCase.findById(5L)).thenReturn(Optional.of(inv));
        when(repo.findByStudentId(7L)).thenReturn(List.of(lot(1L, 7L, "30", "30")));

        assertThatThrownBy(() -> service().applyToInvoice(7L, 5L, new BigDecimal("50"), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuffisant");
        verify(invoiceUseCase, never()).applyCredit(any(), any(), any());
    }

    @Test
    void applyToInvoice_appliesFifoAcrossLotsAndRecordsUsage() {
        Invoice inv = Invoice.builder().id(5L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100")).remainingBalance(new BigDecimal("100"))
                .status(InvoiceStatus.UNPAID).build();
        when(invoiceUseCase.findById(5L)).thenReturn(Optional.of(inv));
        StudentCredit lot1 = lot(1L, 7L, "30", "30");
        StudentCredit lot2 = lot(2L, 7L, "50", "50");
        when(repo.findByStudentId(7L)).thenReturn(List.of(lot1, lot2));
        when(repo.lockById(1L)).thenReturn(Optional.of(lot1));
        when(repo.lockById(2L)).thenReturn(Optional.of(lot2));
        when(repo.save(any(StudentCredit.class))).thenAnswer(a -> a.getArgument(0));

        BigDecimal applied = service().applyToInvoice(7L, 5L, new BigDecimal("60"), 42L);

        assertThat(applied).isEqualByComparingTo("60");
        // Premier lot (30, épuisé) puis 30 pris sur le second (50 -> 20 restant) : deux applications
        // de 30 chacune, une par lot consommé.
        verify(invoiceUseCase, times(2)).applyCredit(eq(5L), eq(new BigDecimal("30")), any());
        verify(repo, times(2)).saveUsage(any());
    }

    @Test
    void applyToInvoice_cannotExceedInvoiceRemainingBalance() {
        Invoice inv = Invoice.builder().id(5L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100")).paidAmount(new BigDecimal("80"))
                .remainingBalance(new BigDecimal("20")).status(InvoiceStatus.PARTIAL).build();
        when(invoiceUseCase.findById(5L)).thenReturn(Optional.of(inv));
        StudentCredit lot1 = lot(1L, 7L, "100", "100");
        when(repo.findByStudentId(7L)).thenReturn(List.of(lot1));
        when(repo.lockById(1L)).thenReturn(Optional.of(lot1));
        when(repo.save(any(StudentCredit.class))).thenAnswer(a -> a.getArgument(0));

        // Demande 100 de crédit mais la facture ne doit plus que 20 — seuls 20 doivent être appliqués.
        BigDecimal applied = service().applyToInvoice(7L, 5L, new BigDecimal("100"), 42L);

        assertThat(applied).isEqualByComparingTo("20");
        verify(invoiceUseCase).applyCredit(eq(5L), eq(new BigDecimal("20")), any());
    }
}
