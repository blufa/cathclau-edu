package gn.lacaeducation.platform;

import gn.lacaeducation.platform.domain.ModuleCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards an endpoint (or controller) behind an active module for the current tenant
 * (features/SCHOOL_back_complements.md §M). If the module is inactive, the call is rejected with 403
 * {@code {module, raison:"MODULE_INACTIF"}}. SUPER_ADMIN (no tenant scope) always passes.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireModule {
    ModuleCode value();
}
