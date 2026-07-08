package gn.lacaeducation.platform.adapters.in.security;

import gn.lacaeducation.common.ModuleInactiveException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.ModuleAccessProvider;
import gn.lacaeducation.platform.RequireModule;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.JoinPoint;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Enforces {@link RequireModule}: before a guarded endpoint runs, checks the current tenant has the
 * required module active, otherwise throws {@link ModuleInactiveException} (→ 403). SUPER_ADMIN calls
 * (no tenant in {@link TenantContext}) are allowed through by {@link ModuleAccessProvider}.
 */
@Aspect
@Component
public class ModuleGuardAspect {

    private final ModuleAccessProvider moduleAccessProvider;

    public ModuleGuardAspect(ModuleAccessProvider moduleAccessProvider) {
        this.moduleAccessProvider = moduleAccessProvider;
    }

    @Before("@within(gn.lacaeducation.platform.RequireModule) || @annotation(gn.lacaeducation.platform.RequireModule)")
    public void check(JoinPoint joinPoint) {
        RequireModule req = resolve(joinPoint);
        if (req == null) return;
        if (!moduleAccessProvider.isModuleActive(TenantContext.get(), req.value())) {
            throw new ModuleInactiveException(req.value().name());
        }
    }

    /** Method-level annotation takes precedence over the class-level one. */
    private RequireModule resolve(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireModule onMethod = AnnotatedElementUtils.findMergedAnnotation(method, RequireModule.class);
        if (onMethod != null) return onMethod;
        return AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), RequireModule.class);
    }
}
