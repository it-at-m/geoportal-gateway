package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import de.swm.lhm.geoportal.gateway.shared.ErrorHandlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.USERNAME;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.CLIENT_SECRET;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.GRANT_TYPE;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.PASSWORD;

@RequiredArgsConstructor
@Service
@Slf4j
public class KeyCloakService extends ErrorHandlingService {

    private static final String CLIENT_CREDENTIALS_ERROR_MESSAGE = "Failed to generate client credentials granted access token";

    private final WebClient.Builder webClientBuilder;
    private final KeyCloakProviderProperties keyCloakProviderProperties;
    private final KeyCloakRegistrationProperties keyCloakRegistrationProperties;

    public Mono<Authentication> authenticateAndGetPrincipal(String userName, String password) {
        return authenticateWithCredentials(userName, password)
                .flatMap(loginResponse -> getUserInfo(loginResponse.getAccessToken()))
                .map(AuthenticationMapper::mapUserInfoToPrincipal)
                .map(AuthenticationMapper::mapPrincipalToToken);
    }

    public Mono<String> getClientCredentialsGrantedAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        formData.add(CLIENT_ID, keyCloakRegistrationProperties.getClientId());
        formData.add(CLIENT_SECRET, keyCloakRegistrationProperties.getClientSecret());

        return sendTokenRequest(formData, CLIENT_CREDENTIALS_ERROR_MESSAGE, "getClientCredentialsGrantedAccessToken")
                .map(LoginResponse::getAccessToken)
                .onErrorMap(Exception.class, e -> handleException(CLIENT_CREDENTIALS_ERROR_MESSAGE, e))
                .switchIfEmpty(Mono.error(new BadCredentialsException(CLIENT_CREDENTIALS_ERROR_MESSAGE)));
    }

    protected Mono<LoginResponse> sendTokenRequest(MultiValueMap<String, String> formData, String errorMessage, String context) {
        return webClientBuilder.baseUrl(keyCloakProviderProperties.getTokenUri())
                .build()
                .post()
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        clientResponse -> handleErrorResponse(clientResponse.statusCode(), errorMessage, clientResponse, context))
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> handleErrorResponse(clientResponse.statusCode(), "Server error during token request to Keycloak", clientResponse, context))
                .bodyToMono(LoginResponse.class);
    }

    protected Mono<LoginResponse> authenticateWithCredentials(String userName, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, PASSWORD);
        formData.add(USERNAME, userName);
        formData.add(PASSWORD, password);
        formData.add(CLIENT_ID, keyCloakRegistrationProperties.getClientId());
        formData.add(CLIENT_SECRET, keyCloakRegistrationProperties.getClientSecret());

        String errorMessage = String.format("User %s was not successfully authenticated by Keycloak", userName);

        return sendTokenRequest(formData, errorMessage, "authenticateWithCredentials")
                .onErrorMap(Exception.class, e -> handleException(String.format("Failed to authenticate user %s at Keycloak", userName), e));
    }

    protected Mono<UserInfo> getUserInfo(String accessToken) {
        return webClientBuilder.baseUrl(keyCloakProviderProperties.getUserInfoUri())
                .build()
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        clientResponse -> handleErrorResponse(clientResponse.statusCode(), "Failed to fetch user infos from Keycloak", clientResponse, "getUserInfo"))
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> handleErrorResponse(clientResponse.statusCode(), "Server error when fetching user infos from Keycloak", clientResponse, "getUserInfo"))
                .bodyToMono(UserInfo.class)
                .onErrorMap(Exception.class, e -> handleException("Failed to fetch user infos from Keycloak", e))
                .onErrorReturn(new UserInfo());
    }
}