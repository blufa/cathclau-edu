/**
 * Module de reporting & analytics pour l'administration de l'école (§10). Compose les API publiques
 * des modules métier (grading, billing, payments, attendance, staff…) en lecture seule, sans être une
 * dépendance d'aucun d'eux — évitant tout cycle. Réservé aux rôles d'administration/direction.
 */
@org.springframework.modulith.ApplicationModule
package gn.lacaeducation.reporting;
