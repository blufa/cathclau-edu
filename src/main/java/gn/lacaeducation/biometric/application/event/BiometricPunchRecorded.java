package gn.lacaeducation.biometric.application.event;

import java.time.LocalDateTime;

/**
 * Publié quand un pointage biométrique est ingéré et rattaché à une personne. Consommé par le module
 * attendance pour créer la présence. Types primitifs (subjectType en String) pour ne pas exposer les
 * enums internes du module biometric.
 */
public record BiometricPunchRecorded(Long tenantId, String subjectType, Long subjectId,
                                     LocalDateTime punchTime, String direction) {}
