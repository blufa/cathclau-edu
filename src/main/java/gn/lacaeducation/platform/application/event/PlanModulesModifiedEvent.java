package gn.lacaeducation.platform.application.event;

import gn.lacaeducation.platform.domain.ModuleCode;

/** Published when a plan's module set changes (features/SCHOOL_back_complements.md §M). */
public record PlanModulesModifiedEvent(Long planId, ModuleCode moduleCode, boolean active) {}
