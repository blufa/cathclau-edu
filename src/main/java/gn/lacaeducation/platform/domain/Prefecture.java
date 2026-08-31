package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Périmètre géographique de supervision DPE/DCE (voir DPE_ARCHITECTURE_ANALYSIS.md §5-7). Les
 * écoles ({@link Tenant#getPrefectureId()}) et les comptes DPE_ADMIN (table {@code
 * dpe_user_prefectures}) s'y rattachent ; le module de dashboard/statistiques DPE (à venir) résout
 * son périmètre de lecture à partir de cette entité, jamais en modifiant l'isolation tenant classique.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prefecture {
    private Long id;
    private String nom;
    private PrefectureType type;
    private String region;
    private String pays;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private PrefectureStatus statut;
    private LocalDateTime createdAt;
}
