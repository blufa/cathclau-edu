package gn.lacaeducation.communication.adapters.out.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Active la liaison de {@link NimbaSmsProperties} pour le module communication. */
@Configuration
@EnableConfigurationProperties(NimbaSmsProperties.class)
class NimbaSmsConfig {
}
