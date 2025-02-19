package de.swm.lhm.geoportal.gateway.authorization.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;

@NoArgsConstructor
@ToString
@Getter
@Setter
public class AuthorizationInfo {

    @Column("role_name")
    private String roleName;

    @Column("auth_level_high")
    private boolean authLevelHigh;

    @Column("access_level")
    private AccessLevel accessLevel;

    @Column("resource_id")
    private String resourceId;

    public boolean isPublic() {
        return accessLevel == AccessLevel.PUBLIC;
    }


}
