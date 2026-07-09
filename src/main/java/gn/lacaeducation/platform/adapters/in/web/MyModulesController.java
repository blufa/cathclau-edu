package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.ModuleAccessProvider;
import gn.lacaeducation.platform.domain.ModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Active modules for the currently authenticated tenant — consumed by the front to enable/grey out
 * menus. During an active free trial every module is returned; once the trial ends only the modules
 * granted by the paid plan remain. SUPER_ADMIN (no tenant) gets the full catalogue.
 */
@Tag(name = "My Modules", description = "Active modules of the current tenant")
@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
public class MyModulesController {

    private final ModuleAccessProvider moduleAccessProvider;

    @Operation(summary = "List the active module codes for the current tenant")
    @GetMapping("/active")
    public List<String> activeModules() {
        return moduleAccessProvider.getActiveModules(TenantContext.get()).stream().map(ModuleCode::name).toList();
    }
}
