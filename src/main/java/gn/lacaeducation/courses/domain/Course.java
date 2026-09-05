package gn.lacaeducation.courses.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cours payant (vidéo/PDF) rattaché à une classe (§16 B — Mathématiques, Chimie...). Chaque cours
 * possède un {@link #schoolFeeId} créé automatiquement, ce qui réutilise entièrement la facturation
 * et le paiement (Djomy) déjà en place pour les frais scolaires plutôt que de dupliquer un circuit
 * de paiement dédié.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Course {
    private Long id;
    private Long tenantId;
    private Long classId;
    private String subjectLabel;
    private String title;
    private String description;
    private BigDecimal price;
    private String thumbnailUrl;
    private Long schoolFeeId;
    private boolean published;
    private LocalDateTime createdAt;
}
