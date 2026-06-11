package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.CreateTenantRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.TenantResponse;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return TenantResponse.from(createTenantUseCase.create(
            new CreateTenantUseCase.CreateTenantCommand(
                request.schoolName(), request.email(), request.phone(),
                request.address(), request.city(), request.country(), request.director()
            )
        ));
    }

    @GetMapping
    public List<TenantResponse> findAll() {
        return createTenantUseCase.findAll().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse findById(@PathVariable Long id) {
        return TenantResponse.from(createTenantUseCase.findById(id));
    }

    @PutMapping("/{id}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long id) {
        createTenantUseCase.suspend(id);
    }
}
