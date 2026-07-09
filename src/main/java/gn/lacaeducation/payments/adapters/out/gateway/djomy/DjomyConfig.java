package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables {@link DjomyProperties} binding for the payments module. */
@Configuration
@EnableConfigurationProperties(DjomyProperties.class)
class DjomyConfig {
}
