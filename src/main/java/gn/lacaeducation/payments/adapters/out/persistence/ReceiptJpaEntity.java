package gn.lacaeducation.payments.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "receipts") @Getter @Setter
public class ReceiptJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_id", nullable = false, unique = true) private Long paymentId;
    @Column(unique = true, nullable = false) private String reference;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "pdf_file") private String pdfFile;
    @Column(name = "generated_at", nullable = false) private LocalDateTime generatedAt;
}
