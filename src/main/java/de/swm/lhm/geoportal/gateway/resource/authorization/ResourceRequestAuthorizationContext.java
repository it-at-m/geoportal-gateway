package de.swm.lhm.geoportal.gateway.resource.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.resource.model.FileResourcePath;
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
public class ResourceRequestAuthorizationContext {

    public static final String KEY = "portalRequestAuthContext";

    private FileResourcePath fileResourcePath;

    @Builder.Default
    private AuthorizationGroup configuredAuthorizationGroup = AuthorizationGroup.empty();

    @Builder.Default
    private AuthorizationGroup layerAuthorizationGroup = AuthorizationGroup.empty();

}