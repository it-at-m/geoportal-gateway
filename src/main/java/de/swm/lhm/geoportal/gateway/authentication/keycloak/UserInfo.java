package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTHORITIES;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTH_LEVEL;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.PREFERRED_USERNAME;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.SUBJECT;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.USER_NAME;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

    @JsonProperty(AUTHORITIES)
    private List<String> authorities = List.of();

    @JsonProperty(AUTH_LEVEL)
    private StorkQaaLevel authLevel = StorkQaaLevel.STORK_QAA_LEVEL_1;

    @JsonProperty(USER_NAME)
    private String userName;

    @JsonProperty(PREFERRED_USERNAME)
    private String preferredUserName;

    @JsonProperty(SUBJECT)
    private String subject;
}
