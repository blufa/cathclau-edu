package gn.lacaeducation.common;

/**
 * Thrown when a tenant calls an endpoint of a module that is not active for them
 * (features/SCHOOL_back_complements.md §M). Mapped to 403 with {@code {module, raison:"MODULE_INACTIF"}}.
 */
public class ModuleInactiveException extends RuntimeException {
    private final String moduleCode;

    public ModuleInactiveException(String moduleCode) {
        super("Module inactif pour cet établissement : " + moduleCode);
        this.moduleCode = moduleCode;
    }

    public String getModuleCode() {
        return moduleCode;
    }
}
