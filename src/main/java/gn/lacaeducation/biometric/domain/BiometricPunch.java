package gn.lacaeducation.biometric.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un pointage capté par un appareil et ingéré (avec la personne résolue via l'enrôlement). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricPunch {
    private Long id;
    private Long tenantId;
    private Long deviceId;
    private String externalUserId;
    private BiometricSubjectType subjectType;
    private Long subjectId;
    private LocalDateTime punchTime;
    private PunchDirection direction;
    private BigDecimal temperature;
    private String rawPayload;
    private LocalDateTime createdAt;
}
