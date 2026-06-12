package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase.UpdateScaleCommand;
import gn.lacaeducation.grading.domain.GradingScale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Grading Scale", description = "Per-tenant honor/decision grading scale")
@RestController
@RequestMapping("/api/v1/grading/scale")
@RequiredArgsConstructor
public class GradingScaleController {

    private final GradingScaleUseCase useCase;

    @Operation(summary = "Get the current tenant's grading scale (defaults if not configured)")
    @GetMapping
    public GradingScale get() {
        return useCase.getForTenant(TenantContext.get());
    }

    @Operation(summary = "Set the current tenant's grading scale (mention bands + pass threshold)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @PutMapping
    public GradingScale update(@RequestBody UpdateScaleCommand command) {
        return useCase.update(TenantContext.get(), command);
    }
}
