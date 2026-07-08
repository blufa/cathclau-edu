package gn.lacaeducation.grading.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Étapes du workflow de validation académique des bulletins (§10), séquentielles. Chaque étape ne
 * peut être franchie que par un rôle autorisé (« signature » du bon niveau).
 */
public enum WorkflowStage {
    DRAFT(Set.of()),
    SUBMITTED(Set.of("TEACHER", "SCHOOL_ADMIN", "DIRECTOR")),
    DEPARTMENT_APPROVED(Set.of("RESPONSABLE_PEDAGOGIQUE", "SCHOOL_ADMIN", "DIRECTOR")),
    STUDIES_APPROVED(Set.of("DIRECTOR", "SCHOOL_ADMIN")),
    PRINCIPAL_APPROVED(Set.of("SCHOOL_ADMIN")),
    PUBLISHED(Set.of("SCHOOL_ADMIN", "DIRECTOR")),
    LOCKED(Set.of("SCHOOL_ADMIN", "DIRECTOR"));

    private static final List<WorkflowStage> ORDER = List.of(values());

    private final Set<String> requiredRoles;

    WorkflowStage(Set<String> requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    /** Rôles autorisés à faire franchir cette étape (vide = étape initiale). */
    public Set<String> requiredRoles() {
        return requiredRoles;
    }

    public boolean isTerminal() {
        return this == LOCKED;
    }

    /** Étape suivante dans le flux, ou vide si étape terminale. */
    public Optional<WorkflowStage> next() {
        int i = ORDER.indexOf(this);
        return i < 0 || i + 1 >= ORDER.size() ? Optional.empty() : Optional.of(ORDER.get(i + 1));
    }
}
