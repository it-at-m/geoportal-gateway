package de.swm.lhm.geoportal.gateway.portal.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalRequestAuthorizationContext {

    public static final String KEY_PORTAL_REQUEST_AUTH_CONTEXT = "portalRequestAuthContext";

    private String portalName;

    @Builder.Default
    private AuthorizationGroup authorizationGroup = AuthorizationGroup.empty();

}
