package gn.lacaeducation.calendar;

import java.time.LocalDate;
import java.util.List;

/** API publique du module calendar : jours non travaillés (fériés/vacances) pour les calculs de
 * jours ouvrés hors du module (ex. paie §6 — heures dues d'un enseignant sur un mois donné). */
public interface CalendarDataProvider {

    /** Une entrée par jour calendaire (pas par plage) des jours fériés/vacances du tenant qui
     * recoupent [from, to], pour une exclusion directe dans un comptage jour par jour. */
    List<LocalDate> holidayDates(Long tenantId, LocalDate from, LocalDate to);
}
