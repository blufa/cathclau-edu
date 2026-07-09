package gn.lacaeducation.students.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Minimal cross-school directory record (features §E): enough to verify a transferred student. */
@Getter
@AllArgsConstructor
public class StudentDirectoryEntry {
    private final String firstName;
    private final String lastName;
    private final String studentNumber;
    private final LocalDate birthDate;
    private final String originSchool;   // free-text previous school entered on the record
    private final String schoolName;     // current registered school (tenant) — the "origin" for school B
    private final String lastClass;      // most recent enrolled class label
    private final String lastDecision;   // latest report-card decision (promotion result), if any
    private final int unpaidMonths;      // nb de mensualités non soldées (régularité de paiement)
    private final BigDecimal overallAverage; // dernière moyenne générale au bulletin, si disponible
}
