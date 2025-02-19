package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider.keycloak")
@NoArgsConstructor
@Setter
@Getter
public class KeyCloakProviderProperties {
    String tokenUri;
    String userInfoUri;
    String logoutUri;
}
