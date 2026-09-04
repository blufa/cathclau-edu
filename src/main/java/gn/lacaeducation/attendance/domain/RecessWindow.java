package gn.lacaeducation.attendance.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Fenêtre de récréation quotidienne définie par l'école (ex. 10h00–10h15). Si le début d'une séance
 * tombe dans cette fenêtre, l'enseignant n'est attendu qu'à la fin de la récréation — ce temps est
 * considéré réalisé, pas imputable à l'enseignant.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RecessWindow {
    private Long id;
    private Long tenantId;
    private String label;
    private LocalTime startTime;
    private LocalTime endTime;

    public boolean contains(LocalTime t) {
        return startTime != null && endTime != null && t != null
                && !t.isBefore(startTime) && t.isBefore(endTime);
    }
}
