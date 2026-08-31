package gn.lacaeducation.academics;

import java.time.LocalDate;

/** Année scolaire d'un tenant (pour les documents/objets dont la validité suit l'année, ex. cartes
 * élèves, ou pour dériver la durée réelle de l'année scolaire, ex. projection de paie). */
public record AcademicYearPublicInfo(Long id, String label, LocalDate startDate, LocalDate endDate) {}
