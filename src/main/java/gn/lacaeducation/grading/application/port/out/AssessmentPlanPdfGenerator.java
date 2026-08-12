package gn.lacaeducation.grading.application.port.out;

import java.util.List;

/**
 * Génère le PDF de la fiche de notes vierge (planning des évaluations, §documents ministère) : liste
 * des élèves de la classe avec des colonnes vides (mois, moyenne de cours, de composition) que
 * l'enseignant remplit à la main puis restitue — imprimée et distribuée à chaque enseignant.
 */
public interface AssessmentPlanPdfGenerator {

    byte[] generate(AssessmentPlan plan);

    record AssessmentPlan(Long tenantId, String className, String subjectName, String schoolYear,
                          List<String> studentNames) {}
}
