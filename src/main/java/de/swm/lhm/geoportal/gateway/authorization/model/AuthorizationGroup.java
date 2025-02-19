package de.swm.lhm.geoportal.gateway.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authorization.AuthorizationDecision;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class AuthorizationGroup {

    @Builder.Default
    private Collection<String> productRoles = new HashSet<>();
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.PUBLIC;
    @Builder.Default
    private boolean authLevelHigh = false;
    private String resourceId;
    @Builder.Default
    private Boolean hasPrincipal = null;

    public static AuthorizationGroup fromAccessInfos(List<AuthorizationInfo> accessInfos) {
        AuthorizationGroup result = new AuthorizationGroup();
        accessInfos.forEach(result::addAuthorizationInfo);
        return result;
    }

    public static AuthorizationGroup empty() {
        return new AuthorizationGroup();
    }

    public static AuthorizationDecision isAuthorized(Object potAccessInfoGroup, Collection<String> grantedRoles, boolean userHasAuthLevelHigh) {
        if (potAccessInfoGroup instanceof AuthorizationGroup authorizationGroup) {
            if (authorizationGroup.isPublic()) {
                return new AuthorizationDecision(true);
            }
            if (authorizationGroup.isAuthLevelHigh() && !userHasAuthLevelHigh) {
                return new AuthorizationDecision(false);
            }

            if (
                    grantedRoles != null
                            && !Collections.disjoint(authorizationGroup.getProductRoles(), grantedRoles)
            ) {
                return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }

    private void addAuthorizationInfo(AuthorizationInfo authInfo) {
        if (
                this.resourceId != null
                        && !this.resourceId.isBlank()
                        && !Objects.equals(this.resourceId, authInfo.getResourceId())
        )
            throw new IllegalArgumentException(
                    String.format(
                            "Failed to add AuthorizationInfo %s to AuthorizationGroup %s: resourceId does not match",
                            authInfo,
                            this
                    )
            );

        this.resourceId = authInfo.getResourceId();

        if (!this.isAuthLevelHigh() && authInfo.isAuthLevelHigh()) {
            this.setAuthLevelHigh(true);
        }
        if (StringUtils.isNotBlank(authInfo.getRoleName())) {
            this.addProductRole(authInfo.getRoleName());
        }
        if (this.getAccessLevel().compareTo(authInfo.getAccessLevel()) < 0) {
            this.setAccessLevel(authInfo.getAccessLevel());
        }

    }

    public void addProductRole(String role) {
        this.productRoles.add(role);
    }

    public boolean isPublic() {
        return accessLevel == AccessLevel.PUBLIC;
    }
}
