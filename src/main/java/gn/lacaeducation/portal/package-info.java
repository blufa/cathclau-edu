/**
 * Module d'agrégation lecture seule pour les portails (parent en premier). Il compose les API
 * publiques des modules métier (students, academics, grading, billing) sans être une dépendance
 * d'aucun d'eux, évitant tout cycle.
 */
@org.springframework.modulith.ApplicationModule
package gn.lacaeducation.portal;
