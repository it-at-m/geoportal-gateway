package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.keycloak")
@NoArgsConstructor
@Setter
@Getter
public class KeyCloakRegistrationProperties {
    String clientId;
    String clientSecret;
}
