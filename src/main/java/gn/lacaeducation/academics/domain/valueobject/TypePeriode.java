package gn.lacaeducation.academics.domain.valueobject;

/**
 * Type de découpage d'une {@code Periode}. MENSUEL/BIMENSUEL (§censeur) sont des périodes
 * additionnelles, créées librement par l'établissement en plus de ses trimestres/semestres — elles
 * servent à générer un bulletin de contrôle mensuel (niveau de l'élève en cours d'année), jamais
 * incluses dans la moyenne annuelle officielle (filtrée sur SEMESTRE uniquement, voir
 * {@code ReportCardService}/{@code AnnualReportCardService}).
 */
public enum TypePeriode { TRIMESTRE, SEMESTRE, MENSUEL, BIMENSUEL }
