package gn.lacaeducation.payments.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Receipt {
    private Long id;
    private Long paymentId;
    private String reference;
    private BigDecimal amount;
    private String pdfFile;
    private LocalDateTime generatedAt;
    /** Reçu groupé : mois couverts (ex. « Octobre, Novembre » ou « Année complète »), null si simple. */
    private String coveredMonths;
    /** Nombre de mois réglés par ce reçu groupé. */
    private Integer monthsCount;
    /** Reste à payer (scolarité) après ce règlement, imprimé sur le reçu. */
    private BigDecimal remainingAfter;
    /** Nom de l'élève payeur, figé au moment de l'émission (snapshot) pour l'impression du reçu. */
    private String studentName;
}
