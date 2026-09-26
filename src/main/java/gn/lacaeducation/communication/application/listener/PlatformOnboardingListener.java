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
import org.springframework.scheduling.annotation.Async;
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

    @Value("${app.platform.verification-url:https://ecole.jeccamtech.com/verify-email}")
    private String verificationUrl;

    @Value("${app.platform.login-url:https://ecole.jeccamtech.com/login}")
    private String loginUrl;

    @TransactionalEventListener
    @Async
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
     * Envoie les identifiants de connexion (mot de passe temporaire) à l'administrateur de l'école
     * créée manuellement par le SUPER_ADMIN sans mot de passe fourni — par SMS s'il a un numéro
     * (message bref, la connexion se fait par numéro + mot de passe) ET par email s'il a une adresse :
     * les deux canaux sont indépendants, comme pour {@code CommunicationEventListener.onUserAccountCreated}.
     */
    @TransactionalEventListener
    @Async
    public void onTenantAdminCredentialsIssued(TenantAdminCredentialsIssuedEvent event) {
        boolean hasEmail = event.adminEmail() != null && !event.adminEmail().isBlank();
        boolean hasPhone = event.adminPhone() != null && !event.adminPhone().isBlank();

        if (hasPhone) {
            // Bref, pas d'identifiant à retenir : la connexion se fait par numéro + mot de passe.
            String smsBody = "Votre espace pour l'école \"" + event.schoolName() + "\" est prêt. "
                    + "Connectez-vous avec votre numéro et le mot de passe " + event.temporaryPassword()
                    + " : " + loginUrl;
            sendSms(event.tenantId(), smsBody, event.adminPhone());
        }
        if (hasEmail) {
            String body = "Votre espace pour l'école \"" + event.schoolName() + "\" est prêt.\n\n"
                    + "Identifiants de connexion :\n"
                    + "• Adresse : " + loginUrl + "\n"
                    + "• Email : " + event.adminEmail() + "\n"
                    + "• Mot de passe temporaire : " + event.temporaryPassword() + "\n\n"
                    + "Pour votre sécurité, ce mot de passe est temporaire : vous devrez le changer lors de "
                    + "votre première connexion.";
            send(event.tenantId(), "Vos identifiants de connexion — " + event.schoolName(), body, event.adminEmail());
        }
    }

    @TransactionalEventListener
    @Async
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

    private void sendSms(Long tenantId, String body, String phone) {
        try {
            communicationUseCase.send(new SendCommunicationCommand(
                    tenantId, null, CommunicationChannel.SMS, RecipientType.GROUP,
                    "Vos identifiants de connexion", body, CommunicationReason.GENERAL,
                    List.of(new RecipientTarget(null, phone))));
        } catch (Exception e) {
            log.warn("Failed to send onboarding SMS to {} for tenant {}: {}", phone, tenantId, e.getMessage());
        }
    }
}
