package gn.lacaeducation.calendar;

import java.time.LocalDate;
import java.util.List;

/** API publique du module calendar : jours non travaillés (fériés/vacances) pour les calculs de
 * jours ouvrés hors du module (ex. paie §6 — heures dues d'un enseignant sur un mois donné).
 * Distingue volontairement deux types, traités différemment en paie :
 * <ul>
 *   <li>{@link #paidHolidayDates} — jours fériés CHÔMÉS PAYÉS (ex. fête nationale) : toujours comptés
 *       comme travaillés, jamais déduits des heures dues, quel que soit le statut de paiement
 *       pendant les vacances de l'enseignant ;</li>
 *   <li>{@link #vacationDates} — vacances scolaires : déduites des heures dues par défaut, sauf mois
 *       précis marqué payé pendant les vacances pour l'enseignant concerné (voir
 *       payroll.TeacherVacationPayMonth).</li>
 * </ul>
 */
public interface CalendarDataProvider {

    /** Une entrée par jour calendaire (pas par plage) des jours fériés chômés payés du tenant qui
     * recoupent [from, to] — toujours payés, jamais exclus du décompte des heures dues. */
    List<LocalDate> paidHolidayDates(Long tenantId, LocalDate from, LocalDate to);

    /** Une entrée par jour calendaire (pas par plage) des vacances scolaires du tenant qui recoupent
     * [from, to] — exclues du décompte des heures dues sauf mois payé explicitement. */
    List<LocalDate> vacationDates(Long tenantId, LocalDate from, LocalDate to);
}
