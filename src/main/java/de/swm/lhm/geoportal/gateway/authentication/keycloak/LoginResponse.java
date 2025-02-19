package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    @JsonSetter("access_token")
    private String accessToken;
    @JsonSetter("expires_in")
    private long accessTokenExpiresIn;
    @JsonSetter("refresh_token")
    private String refreshToken;
    @JsonSetter("refresh_expires_in")
    private long refreshTokenExpiresIn;
    @JsonSetter("token_type")
    private String tokenType;
    @JsonSetter("not-before-policy")
    private int notBeforePolicy;
    @JsonSetter("session_state")
    private String sessionState;
    @JsonSetter("scope")
    private String scope;
    @JsonSetter("id_token")
    private String idToken;
}
