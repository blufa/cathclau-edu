package gn.lacaeducation.dpe.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpeAlert {
    private Long id;
    private Long prefectureId;
    private Long tenantId;
    private DpeAlertType type;
    private DpeAlertPriority priorite;
    private String description;
    private DpeAlertStatus statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Long resolvedByUserId;
}
