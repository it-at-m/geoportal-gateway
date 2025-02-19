package de.swm.lhm.geoportal.gateway.authentication.login;

import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.util.HtmlServeUtils.createHtmlResponse;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginLogoutProperties loginLogoutProperties;
    private final IAuthService authorizationService;
    private final ResponseEntity<String> userIsNotAuthenticated = ResponseEntity.status(403).body("User is not authenticated");

    @GetMapping("${geoportal.gateway.login.logincheck-endpoint}")
    public Mono<ResponseEntity<String>> checkAuthentication() {
        // Retrieve authentication information using ReactiveSecurityContextHolder
        return authorizationService.getAuthentication()
                .map(authentication -> {
                    // Check if the authentication is not null and is authenticated
                    if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                        return ResponseEntity.ok("User is authenticated");
                    } else {
                        return userIsNotAuthenticated;
                    }
                })
                .defaultIfEmpty(userIsNotAuthenticated);
    }

    @GetMapping("${geoportal.gateway.login.login-success.endpoint}")
    public Mono<Void>  loginSuccess(ServerWebExchange exchange) {
        return createHtmlResponse(exchange, loginLogoutProperties.getLoginSuccess().getPage(), HttpStatus.OK);
    }

}
