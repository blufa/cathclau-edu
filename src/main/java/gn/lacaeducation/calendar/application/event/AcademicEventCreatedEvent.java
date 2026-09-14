package gn.lacaeducation.calendar.application.event;

import java.time.LocalDate;

/**
 * Publié uniquement à la CRÉATION d'un événement du calendrier académique (pas à la modification, pour
 * ne pas notifier à chaque correction mineure de date/description). Consommé par {@code communication}
 * pour diffuser une notification à tous les parents de l'établissement. {@code eventType} est passé en
 * {@code String} (nom de l'enum) — la frontière du module {@code calendar} n'expose pas son enum
 * domaine aux autres modules, même pattern que {@code StudentAbsentEvent} dans {@code attendance}.
 */
public record AcademicEventCreatedEvent(Long tenantId, Long eventId, String title, String eventType,
                                        LocalDate startDate, String description) {}
