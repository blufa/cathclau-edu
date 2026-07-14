package gn.lacaeducation.iam.adapters.in.security;

import gn.lacaeducation.iam.application.port.out.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enregistre l'activité de l'utilisateur authentifié à chaque requête, pour dériver un statut
 * « en ligne » côté vue plateforme (SUPER_ADMIN) sans dépendre du seul {@code last_login_at} (qui ne
 * bouge qu'à la connexion, pas pendant toute la durée d'une session encore active).
 *
 * <p>L'écriture en base est throttlée en mémoire (au plus une par utilisateur par minute) pour ne pas
 * transformer chaque requête en écriture — un cache en mémoire suffit ici, une précision à la minute
 * près est largement suffisante pour un indicateur « en ligne / hors ligne ».</p>
 *
 * <p>La dépendance est résolue en différé via {@link ObjectProvider} : dans un test de slice web
 * (ex. {@code @WebMvcTest}), la couche de persistance n'est pas chargée et aucun bean
 * {@link UserRepository} n'existe — le filtre doit rester un no-op silencieux plutôt que de faire
 * échouer le démarrage du contexte Spring.</p>
 */
@Component
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {

    private static final Duration THROTTLE = Duration.ofSeconds(60);

    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ConcurrentHashMap<Long, Instant> lastTouched = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Long userId = parseUserId(jwtAuth.getToken().getSubject());
                if (userId != null) {
                    Instant now = Instant.now();
                    Instant previous = lastTouched.put(userId, now);
                    if (previous == null || previous.isBefore(now.minus(THROTTLE))) {
                        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
                        if (userRepository != null) {
                            try {
                                userRepository.touchActivity(userId);
                            } catch (RuntimeException ignored) {
                                // Best-effort : une écriture de télémétrie ratée ne doit jamais faire échouer la requête.
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // idem : ce filtre est purement accessoire.
        }
        filterChain.doFilter(request, response);
    }

    private static Long parseUserId(String subject) {
        try {
            return subject != null ? Long.valueOf(subject) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
