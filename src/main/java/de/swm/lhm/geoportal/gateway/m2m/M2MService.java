package de.swm.lhm.geoportal.gateway.m2m;

import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakService;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.m2m.model.EaiResponse;
import de.swm.lhm.geoportal.gateway.m2m.model.M2MCredentials;
import de.swm.lhm.geoportal.gateway.shared.ErrorHandlingService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.security.InvalidParameterException;

@Service
@Slf4j
@RequiredArgsConstructor
public class M2MService extends ErrorHandlingService {

    private final WebClient.Builder webClientBuilder;
    private final M2MProperties m2mProperties;
    private final IAuthService authorizationService;
    private final KeyCloakService keyCloakService;

    public Mono<M2MCredentials> generatePassword() {
        return Mono.zip(
                    authorizationService.getUserName()
                            .switchIfEmpty(Mono.error(new InvalidParameterException("Could not obtain user name from authorization"))),
                    authorizationService.getSubject()
                            .switchIfEmpty(Mono.error(new InvalidParameterException("Could not obtain subject from authorization"))),
                    keyCloakService.getClientCredentialsGrantedAccessToken()
                            .switchIfEmpty(Mono.error(new InvalidParameterException("Could not obtain client credentials granted access token from keycloak")))
                )
                .flatMap(tuple -> {

                    String userName = tuple.getT1();
                    String subject = tuple.getT2();
                    String token = tuple.getT3();

                    log.debug("Generate Password for user {} with subject {}", userName, subject);

                    return webClientBuilder
                            .baseUrl(getGeneratePasswordUrl(subject))
                            .build()
                            .put()
                            .headers(headers -> headers.setBearerAuth(token))
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .bodyToMono(EaiResponse.class)
                            .map(response -> new M2MCredentials(userName, response.getPassword()))
                            .onErrorMap(WebClientResponseException.class, exception -> handleException("Failed to generate password", exception));

                }).switchIfEmpty(Mono.error(new M2MPasswordException("Failed to create M2M Password")));

    }

    @SneakyThrows
    private String getGeneratePasswordUrl(String subject) {
        return new URIBuilder(m2mProperties.getEaiUrl())
                .setPathSegments("api", "user", subject, "password")
                .build()
                .toString();
    }
}