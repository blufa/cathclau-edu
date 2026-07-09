package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.PlanResponse;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase.CreatePlanCommand;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase.UpdatePlanCommand;
import gn.lacaeducation.platform.application.port.in.PlanQueryUseCase;
import gn.lacaeducation.platform.domain.Periodicity;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.PlanType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@Tag(name = "Plans", description = "Subscription plan catalog")
@RestController
@RequestMapping("/api/v1/platform/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanQueryUseCase planQueryUseCase;
    private final PlanManagementUseCase planManagementUseCase;

    @Operation(summary = "List all plans (public)")
    @GetMapping
    public List<PlanResponse> findAll() {
        return planQueryUseCase.findAll().stream().map(PlanResponse::from).toList();
    }

    @Operation(summary = "Get plan by ID")
    @GetMapping("/{id}")
    public PlanResponse findById(@PathVariable Long id) {
        return PlanResponse.from(planQueryUseCase.findById(id));
    }

    @Operation(summary = "Get plan by code (STARTER/STANDARD/PREMIUM)")
    @GetMapping("/code/{code}")
    public ResponseEntity<PlanResponse> findByCode(@PathVariable PlanCode code) {
        return planManagementUseCase.findByCode(code)
                .map(PlanResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new plan")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody CreatePlanRequest req) {
        return PlanResponse.from(planManagementUseCase.create(
                new CreatePlanCommand(req.name(), req.code(), req.studentLimit(),
                        req.monthlyPrice(), req.annualPrice(), req.description(),
                        req.planType(), req.periodicity(), req.userLimit(), req.smsLimit(),
                        req.publicVisible(), req.displayOrder(), req.highlighted(),
                        req.trial(), req.trialDays())));
    }

    @Operation(summary = "Update an existing plan")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePlanRequest req) {
        return PlanResponse.from(planManagementUseCase.update(id,
                new UpdatePlanCommand(req.name(), req.studentLimit(),
                        req.monthlyPrice(), req.annualPrice(), req.description(),
                        req.planType(), req.periodicity(), req.userLimit(), req.smsLimit(),
                        req.publicVisible(), req.displayOrder(), req.highlighted())));
    }

    @Operation(summary = "Archive a plan (no longer subscribable; existing subscriptions unaffected)")
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse archive(@PathVariable Long id) {
        return PlanResponse.from(planManagementUseCase.archive(id));
    }

    @Operation(summary = "Designate this plan as the default trial (price 0 + trialDays)")
    @PatchMapping("/{id}/definir-trial-defaut")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PlanResponse setDefaultTrial(@PathVariable Long id) {
        return PlanResponse.from(planManagementUseCase.setDefaultTrial(id));
    }

    @Operation(summary = "Delete a plan")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { planManagementUseCase.delete(id); }

    record CreatePlanRequest(String name, PlanCode code, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description,
                              PlanType planType, Periodicity periodicity, Integer userLimit,
                              Integer smsLimit, Boolean publicVisible, Integer displayOrder,
                              Boolean highlighted, Boolean trial, Integer trialDays) {}
    record UpdatePlanRequest(String name, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description,
                              PlanType planType, Periodicity periodicity, Integer userLimit,
                              Integer smsLimit, Boolean publicVisible, Integer displayOrder,
                              Boolean highlighted) {}
}
