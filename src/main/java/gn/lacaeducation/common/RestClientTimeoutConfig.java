package gn.lacaeducation.common;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * Timeout connexion/lecture par défaut pour tous les {@code RestClient} construits à partir du bean
 * Spring {@code RestClient.Builder} (Nimba, Orange SMS, Resend, Djomy — chaque gateway reçoit le
 * builder par injection puis appelle {@code .build()}). Sans ça, un appel réseau bloqué (DNS, tunnel
 * ngrok mal configuré, pare-feu) gèle indéfiniment le thread de la requête HTTP entrante : le backend
 * « ne répond plus du tout » à l'utilisateur, sans aucune trace de la tentative dans les logs — c'est
 * le symptôme observé en testant l'envoi de SMS.
 *
 * <p>N'affecte pas les tests qui construisent leur propre {@code RestClient.builder()} en dehors du
 * contexte Spring (ex. {@code DjomyClientTest} avec {@code MockRestServiceServer}) : ce bean ne
 * s'applique qu'au {@code RestClient.Builder} auto-configuré par Spring Boot.</p>
 */
@Configuration
public class RestClientTimeoutConfig {

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(10));
            requestFactory.setReadTimeout(Duration.ofSeconds(20));
            builder.requestFactory(requestFactory);
        };
    }
}
