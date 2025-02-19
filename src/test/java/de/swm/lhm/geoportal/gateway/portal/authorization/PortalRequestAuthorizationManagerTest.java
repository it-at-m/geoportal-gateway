package de.swm.lhm.geoportal.gateway.portal.authorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


@ExtendWith({OutputCaptureExtension.class})
class PortalRequestAuthorizationManagerTest {

    @Test
    void getAuthorizationDecision(CapturedOutput output) {
        PortalRequestAuthorizationManager manager = new PortalRequestAuthorizationManager(null);
        AuthorizationDecision result = manager.getAuthorizationDecision(null, new AuthorizationContext(null, Map.of(PortalRequestAuthorizationContext.KEY_PORTAL_REQUEST_AUTH_CONTEXT, "")));
        assertThat(result.isGranted(), is(false));
        assertThat(output.getAll(), containsString("ERROR"));
        assertThat(output.getAll(), containsString("Expected a PortalRequestAuthContext but received an object of class String"));
    }
}