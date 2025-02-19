package de.swm.lhm.geoportal.gateway.authentication;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "geoportal.gateway.login")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogoutProperties {

    private String logincheckEndpoint;
    private LoginSuccess loginSuccess;
    private LogoutRedirect logoutRedirect;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccess {
        private String endpoint;
        private String page;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoutRedirect {
        private String endpoint;
        private String uriKey;
        private boolean sendIdToken;
        private String idTokenKey;
    }

}
