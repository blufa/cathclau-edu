package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.PlanResponse;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase.CreatePlanCommand;
import gn.lacaeducation.platform.application.port.in.PlanManagementUseCase.UpdatePlanCommand;
import gn.lacaeducation.platform.application.port.in.PlanQueryUseCase;
import gn.lacaeducation.platform.domain.PlanCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/platform/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanQueryUseCase planQueryUseCase;
    private final PlanManagementUseCase planManagementUseCase;

    @GetMapping
    public List<PlanResponse> findAll() {
        return planQueryUseCase.findAll().stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PlanResponse findById(@PathVariable Long id) {
        return PlanResponse.from(planQueryUseCase.findById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PlanResponse> findByCode(@PathVariable PlanCode code) {
        return planManagementUseCase.findByCode(code)
                .map(PlanResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@RequestBody CreatePlanRequest req) {
        return PlanResponse.from(planManagementUseCase.create(
                new CreatePlanCommand(req.name(), req.code(), req.studentLimit(),
                        req.monthlyPrice(), req.annualPrice(), req.description())));
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable Long id, @RequestBody UpdatePlanRequest req) {
        return PlanResponse.from(planManagementUseCase.update(id,
                new UpdatePlanCommand(req.name(), req.studentLimit(),
                        req.monthlyPrice(), req.annualPrice(), req.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        planManagementUseCase.delete(id);
    }

    record CreatePlanRequest(String name, PlanCode code, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description) {}
    record UpdatePlanRequest(String name, Integer studentLimit,
                              BigDecimal monthlyPrice, BigDecimal annualPrice, String description) {}
}
