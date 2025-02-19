package de.swm.lhm.geoportal.gateway.authentication.keycloak;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTHORITIES;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTH_LEVEL;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.PREFERRED_USERNAME;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.SUBJECT;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.USER_NAME;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

public final class AuthenticationMapper {

    private AuthenticationMapper() {}

    public static OAuth2AuthenticatedPrincipal mapUserInfoToPrincipal(UserInfo userInfo) {

        List<GrantedAuthority> authorities = createAuthorityList(userInfo.getAuthorities());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AUTH_LEVEL, userInfo.getAuthLevel());
        attributes.put(AUTHORITIES, userInfo.getAuthorities());

        attributes.put(USER_NAME, userInfo.getUserName());
        attributes.put(PREFERRED_USERNAME, userInfo.getPreferredUserName());
        attributes.put(SUBJECT, userInfo.getSubject());

        return new DefaultOAuth2AuthenticatedPrincipal(
                        userInfo.getPreferredUserName(),
                        attributes,
                        authorities
                );
    }

    public static Authentication mapPrincipalToToken(OAuth2AuthenticatedPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

}