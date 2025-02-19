package de.swm.lhm.geoportal.gateway.print.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintRequestAuthorizationContext {

    public static final String KEY = "printRequestAuthContext";

    private List<AuthorizationGroup> layerAuthorizationGroups;

}