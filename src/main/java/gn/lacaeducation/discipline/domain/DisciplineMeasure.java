package gn.lacaeducation.discipline.domain;

/** Mesures du règlement intérieur (punitions scolaires et sanctions disciplinaires). */
public enum DisciplineMeasure {
    // ── Punitions scolaires ──
    OBSERVATION,             // observation sur le carnet
    CONVOCATION_FAMILLE,     // convocation des familles
    EXCUSES,                 // excuses orales ou écrites
    DEVOIR_SUPPLEMENTAIRE,   // devoir supplémentaire
    RETENUE,                 // retenue (mercredi/samedi)
    TRAVAUX_INTERET,         // travaux d'intérêt collectif / réparation
    EXCLUSION_COURS,         // exclusion exceptionnelle d'un cours
    // ── Sanctions disciplinaires ──
    AVERTISSEMENT_ORAL,
    AVERTISSEMENT_ECRIT,
    EXCLUSION_INCLUSION,     // exclusion de la classe (exclusion-inclusion)
    BLAME,
    MESURE_RESPONSABILISATION, // ≤ 20 h, activités éducatives
    EXCLUSION_TEMPORAIRE,    // 8 j (chef) ou 1 mois (conseil)
    EXCLUSION_DEFINITIVE,
    AUTRE
}
