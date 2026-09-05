package gn.lacaeducation.courses.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoursePurchase {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long courseId;
    private Long invoiceId;
    private CoursePurchaseStatus status;
    private LocalDateTime createdAt;
    /** Renseigné dès que la facture est soldée (InvoicePaidEvent) — débloque l'accès. */
    private LocalDateTime purchasedAt;
}
