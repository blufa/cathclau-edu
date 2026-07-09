package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.ResendVerificationRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.SchoolBrandingResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.SignupRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.SignupResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.VerifyEmailRequest;
import gn.lacaeducation.platform.application.port.in.SignupUseCase;
import gn.lacaeducation.platform.application.port.in.SignupUseCase.SignupCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Public, unauthenticated endpoints for self-service school onboarding (features/01-platform.md §1):
 * signup, email verification, resend, and branding by slug.
 */
@Tag(name = "Public Signup", description = "Self-service school registration & email verification")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSignupController {

    private final SignupUseCase signupUseCase;

    /** Base URL of the SPA the user is sent back to after clicking the activation link. */
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Operation(summary = "Register a new school (PENDING_VERIFICATION) and email a verification link")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest req) {
        return SignupResponse.from(signupUseCase.signup(new SignupCommand(
                req.nomEcole(), req.slugSouhaite(), req.emailDirecteur(), req.password(),
                req.ville(), req.pays(), req.typeEtablissement(), req.directeur(), req.planSouhaite(),
                req.prenomAdmin(), req.nomAdmin(), req.telephoneAdmin())));
    }

    @Operation(summary = "Verify the director's email (PENDING_VERIFICATION → TRIAL). Idempotent.")
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        signupUseCase.verifyEmail(req.token());
        return ResponseEntity.ok().build();
    }

    /**
     * Clickable GET variant of the activation link embedded in the e-mail. Verifies the school
     * (PENDING_VERIFICATION → TRIAL), then redirects the browser to the SPA so the director lands on
     * the login screen with a success flag and can reach their dashboard. Idempotent.
     */
    @Operation(summary = "Clickable email-activation link (GET): verifies then redirects to the app")
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmailViaLink(@RequestParam String token) {
        boolean ok = true;
        try {
            signupUseCase.verifyEmail(token);
        } catch (RuntimeException e) {
            ok = false;
        }
        String target = frontendUrl + "/login?verified=" + (ok ? "1" : "0");
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    @Operation(summary = "Resend the verification email for a school still pending verification")
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        signupUseCase.resendVerification(req.email());
    }

    @Operation(summary = "Public branding of a school's login page (by slug), without auth")
    @GetMapping("/ecoles/{slug}/branding")
    public SchoolBrandingResponse branding(@PathVariable String slug) {
        return SchoolBrandingResponse.from(signupUseCase.getBranding(slug));
    }
}
