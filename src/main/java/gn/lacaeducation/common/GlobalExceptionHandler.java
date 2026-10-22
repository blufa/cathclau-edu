package gn.lacaeducation.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import java.net.URI;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/validation"));
        pd.setTitle("Validation failed");
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).toList();
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/not-found"));
        pd.setTitle("Resource not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/business-rule"));
        pd.setTitle("Business rule violation");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuota(QuotaExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/quota-exceeded"));
        pd.setTitle("Quota exceeded");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/conflict"));
        pd.setTitle("Conflict");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ClassCapacityExceededException.class)
    public ProblemDetail handleClassCapacityExceeded(ClassCapacityExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/class-capacity-exceeded"));
        pd.setTitle("Capacité de classe atteinte");
        pd.setDetail(ex.getMessage());
        pd.setProperty("classId", ex.getClassId());
        pd.setProperty("currentCount", ex.getCurrentCount());
        pd.setProperty("maxCapacity", ex.getMaxCapacity());
        pd.setProperty("raison", "CAPACITE_ATTEINTE");
        return pd;
    }

    @ExceptionHandler(ModuleInactiveException.class)
    public ProblemDetail handleModuleInactive(ModuleInactiveException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/module-inactive"));
        pd.setTitle("Module inactif");
        pd.setDetail(ex.getMessage());
        pd.setProperty("module", ex.getModuleCode());
        pd.setProperty("raison", "MODULE_INACTIF");
        return pd;
    }

    /** Safety net for DB unique/foreign-key violations not caught earlier (→ 409 instead of 500). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/conflict"));
        pd.setTitle("Conflict");
        pd.setDetail("Conflit de données : une valeur unique existe déjà ou une référence est invalide.");
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/authentication"));
        pd.setTitle("Authentication failed");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/account-disabled"));
        pd.setTitle("Account disabled");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // Trace which endpoint was refused and which authorities the caller actually holds — the raw
        // Spring log ("Resolved AuthorizationDeniedException") gives neither, making 403s hard to diagnose.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authorities = auth == null ? "<anonymous>" : auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList().toString();
        String uri = request.getMethod() + " " + request.getRequestURI();
        log.warn("Access denied on {} — caller authorities={} ({})", uri, authorities, ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/forbidden"));
        pd.setTitle("Access denied");
        pd.setDetail("Vous n'avez pas les droits requis pour " + uri + ".");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArg(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/bad-request"));
        pd.setTitle("Invalid request");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/business-rule"));
        pd.setTitle("Business rule violation");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    /** An upstream HTTP call (payment gateway, etc.) failed — surface it as 502, not a misleading 500. */
    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleUpstream(RestClientResponseException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/upstream"));
        pd.setTitle("Upstream service error");
        pd.setDetail("Upstream call failed with status " + ex.getStatusCode().value()
                + ": " + ex.getResponseBodyAsString());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("https://api.lacaeducation.gn/errors/internal"));
        pd.setTitle("Internal server error");
        return pd;
    }
}
