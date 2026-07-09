package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Association between a plan and a module it includes (features/SCHOOL_back_complements.md §M). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanModule {
    private Long id;
    private Long planId;
    private ModuleCode moduleCode;
    private boolean active;
}
