package de.swm.lhm.geoportal.gateway.base_classes;

import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import de.swm.lhm.geoportal.gateway.base_classes.config.TestSessionConfig;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.Collection;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTHORITIES;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTH_LEVEL;
import static de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel.STORK_QAA_LEVEL_3;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login;

@Import(TestSessionConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest extends SqlRunner {

    public static final int KEYCLOAK_PORT = 9089;

    protected static final String PUBLIC_PRODUCT = "Produkt_public";
    protected static final String PROTECTED_PRODUCT = "Produkt_protected";
    protected static final String PROTECTED_PRODUCT2 = "Produkt2_protected";
    protected static final String PROTECTED_AUTH_LEVEL_HIGH_PRODUCT = "Produkt_protectedhigh";
    protected static final String RESTRICTED_PRODUCT = "Produkt_restricted";

    @Autowired
    protected WebTestClient webTestClient;

    @RegisterExtension
    static KeycloakMockExtension mockKeyCloak = new KeycloakMockExtension(
            ServerConfig.aServerConfig()
                .withNoContextPath()
                .withPort(KEYCLOAK_PORT)
                .withDefaultRealm("public")
                .build()
    );

    /**
     * Mutate a WebTestClient an grant produkts to the request
     * @param grantedProducts collection of all product names
     * @return SecurityMockServerConfigurers.OAuth2LoginMutator
     */
    protected SecurityMockServerConfigurers.OAuth2LoginMutator keyCloakConfigureGrantedProducts(Collection<String> grantedProducts, boolean authLevelHigh) {
        return mockOAuth2Login()
                .attributes(attributes -> {
                    // This is the same way keycloak in the test environment stores the authorities. They are NOT placed
                    // using the authorities(...) method
                    attributes.put(AUTHORITIES, new ArrayList<>(grantedProducts));
                    if (authLevelHigh)
                        attributes.put(AUTH_LEVEL, STORK_QAA_LEVEL_3);
                });
    }

    protected SecurityMockServerConfigurers.OAuth2LoginMutator keyCloakConfigureGrantedProducts(Collection<String> grantedProducts) {
        return keyCloakConfigureGrantedProducts(grantedProducts, false);
    }

    protected void expectRedirectToKeyCloak(WebTestClient.ResponseSpec responseSpec) {
        responseSpec.expectStatus().isFound()
                .expectHeader().location("/oauth2/authorization/keycloak");
    }

}
