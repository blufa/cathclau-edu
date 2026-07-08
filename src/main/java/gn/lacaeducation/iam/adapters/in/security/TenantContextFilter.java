package gn.lacaeducation.iam.adapters.in.security;

import gn.lacaeducation.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantContextFilter extends OncePerRequestFilter {

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
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
