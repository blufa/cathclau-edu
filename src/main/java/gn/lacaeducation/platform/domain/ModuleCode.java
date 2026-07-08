package gn.lacaeducation.platform.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Catalogue of the application's functional modules (features/SCHOOL_back_complements.md §M).
 * A plan includes a set of modules; a tenant's effective modules are derived from its plan and
 * optional per-tenant overrides. {@code premium} flags add-on modules; {@code dependencies} must be
 * active for a module to be enabled (enforced as 409 on activation).
 */
public enum ModuleCode {
    IAM("Utilisateurs & rôles", false),
    STUDENTS("Élèves & parents", false),
    STAFF("Personnel & enseignants", false),
    ACADEMICS("Années, classes, emploi du temps", false),
    ATTENDANCE("Présences", false),
    GRADING("Notes & bulletins", false),
    BILLING("Frais scolaires", false),
    PAYMENTS("Paiements & encaissements", false),
    WEBHOOKS("Intégrations (webhooks)", true),
    REPORT_CARD("Livret scolaire", true),
    COMMUNICATION("SMS / Email / WhatsApp", false),
    EXPENSES("Dépenses & budget", false),
    ACCOUNTING("Comptabilité", true),
    STUDENT_CARDS("Cartes scolaires (QR)", true);

    private final String label;
    private final boolean premium;

    ModuleCode(String label, boolean premium) {
        this.label = label;
        this.premium = premium;
    }

    public String label() {
        return label;
    }

    public boolean premium() {
        return premium;
    }

    /** Modules that must be active for this one to be enabled. */
    public Set<ModuleCode> dependencies() {
        return switch (this) {
            case ATTENDANCE -> EnumSet.of(STUDENTS, ACADEMICS);
            case GRADING -> EnumSet.of(STUDENTS, ACADEMICS);
            case BILLING -> EnumSet.of(STUDENTS);
            case PAYMENTS -> EnumSet.of(BILLING);
            case REPORT_CARD -> EnumSet.of(GRADING);
            case STUDENT_CARDS -> EnumSet.of(STUDENTS);
            case WEBHOOKS -> EnumSet.of(PAYMENTS);
            default -> EnumSet.noneOf(ModuleCode.class);
        };
    }
}
