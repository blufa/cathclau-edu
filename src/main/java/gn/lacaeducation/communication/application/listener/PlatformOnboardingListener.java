package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.RecipientType;
import gn.lacaeducation.platform.application.event.TenantAdminCredentialsIssuedEvent;
import gn.lacaeducation.platform.application.event.TenantOnboardedEvent;
import gn.lacaeducation.platform.application.event.TenantSignupRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Sends the platform onboarding emails to a school's director (features/01-platform.md §1-2): the
 * email-verification link on signup, and the welcome + director guide once onboarding completes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformOnboardingListener {

    private final CommunicationUseCase communicationUseCase;

    @Value("${app.platform.verification-url:https://app.lacaeducation.gn/verify-email}")
    private String verificationUrl;

    @Value("${app.platform.login-url:https://app.lacaeducation.gn/login}")
    private String loginUrl;

    @TransactionalEventListener
    public void onTenantSignupRequested(TenantSignupRequestedEvent event) {
        if (event.adminEmail() == null || event.adminEmail().isBlank()) {
            return;
        }
        String link = verificationUrl + "?token=" + event.verificationToken();
        String body = "Bienvenue sur LACA Education ! Pour activer l'école \"" + event.schoolName()
                + "\", veuillez vérifier votre adresse email en suivant ce lien : " + link;
        send(event.tenantId(), "Vérifiez votre email — " + event.schoolName(), body, event.adminEmail());
    }

    /**
     * Envoie les identifiants de connexion (email + mot de passe temporaire) à l'administrateur de
     * l'école créée manuellement par le SUPER_ADMIN sans mot de passe fourni.
     */
    @TransactionalEventListener
    public void onTenantAdminCredentialsIssued(TenantAdminCredentialsIssuedEvent event) {
        if (event.adminEmail() == null || event.adminEmail().isBlank()) {
            return;
        }
        String body = "Votre espace pour l'école \"" + event.schoolName() + "\" est prêt.\n\n"
                + "Identifiants de connexion :\n"
                + "• Adresse : " + loginUrl + "\n"
                + "• Email : " + event.adminEmail() + "\n"
                + "• Mot de passe temporaire : " + event.temporaryPassword() + "\n\n"
                + "Pour votre sécurité, ce mot de passe est temporaire : vous devrez le changer lors de "
                + "votre première connexion.";
        send(event.tenantId(), "Vos identifiants de connexion — " + event.schoolName(), body, event.adminEmail());
    }

    @TransactionalEventListener
    public void onTenantOnboarded(TenantOnboardedEvent event) {
        if (event.adminEmail() == null || event.adminEmail().isBlank()) {
            return;
        }
        String body = "Votre école \"" + event.schoolName() + "\" est désormais active (période d'essai). "
                + "Vous pouvez gérer élèves, classes, notes et paiements dès maintenant. "
                + "Consultez le guide du directeur pour démarrer.";
        send(event.tenantId(), "Bienvenue — " + event.schoolName(), body, event.adminEmail());
    }

    private void send(Long tenantId, String subject, String body, String email) {
        try {
            communicationUseCase.send(new SendCommunicationCommand(
                    tenantId, null, CommunicationChannel.EMAIL, RecipientType.GROUP,
                    subject, body, CommunicationReason.GENERAL,
                    List.of(new RecipientTarget(null, email))));
        } catch (Exception e) {
            log.warn("Failed to send onboarding email to {} for tenant {}: {}", email, tenantId, e.getMessage());
        }
    }
}
