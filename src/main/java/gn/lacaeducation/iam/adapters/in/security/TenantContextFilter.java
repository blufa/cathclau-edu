package gn.lacaeducation.iam.adapters.in.security;

import gn.lacaeducation.common.DpeScopeContext;
import gn.lacaeducation.common.DpeScopeProvider;
import gn.lacaeducation.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    // Optional: absent in sliced test contexts (e.g. @WebMvcTest) that don't scan the platform
    // module. DPE scope resolution is simply skipped in that case — ordinary tenant users are
    // unaffected either way.
    private final Optional<DpeScopeProvider> dpeScopeProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Object claim = jwtAuth.getToken().getClaim("tenant_id");
                if (claim instanceof Number num) {
                    TenantContext.set(num.longValue());
                } else {
                    // SUPER_ADMIN (no tenant) may impersonate a school for testing/support by sending the
                    // X-Tenant-Id header. Scoped to SUPER_ADMIN only — already an all-powerful role.
                    boolean superAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
                    String header = request.getHeader("X-Tenant-Id");
                    if (superAdmin && header != null && !header.isBlank()) {
                        try {
                            TenantContext.set(Long.parseLong(header.trim()));
                        } catch (NumberFormatException ignored) {
                            // invalid header → behave as platform-level (no tenant)
                        }
                    }
                    // DPE/DCE (no tenant, not SUPER_ADMIN either) : résout le périmètre cross-tenant
                    // depuis les préfectures/communes affectées à ce compte, à chaque requête (jamais
                    // mis en cache dans le JWT) — voir DPE_ARCHITECTURE_ANALYSIS.md §7.
                    boolean dpeAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_DPE_ADMIN") || a.getAuthority().equals("DPE_ADMIN"));
                    if (dpeAdmin && !superAdmin && dpeScopeProvider.isPresent()) {
                        try {
                            Long userId = Long.valueOf(jwtAuth.getToken().getSubject());
                            DpeScopeContext.set(dpeScopeProvider.get().tenantIdsForUser(userId),
                                    dpeScopeProvider.get().prefectureIdsForUser(userId));
                        } catch (NumberFormatException ignored) {
                            // malformed subject → no DPE scope resolved
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            DpeScopeContext.clear();
        }
    }
}
