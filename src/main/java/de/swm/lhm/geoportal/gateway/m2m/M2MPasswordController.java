package de.swm.lhm.geoportal.gateway.m2m;

import de.swm.lhm.geoportal.gateway.m2m.model.M2MCredentials;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Basic Auth / M2M Passwort")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("${geoportal.m2m.password-endpoint}")
public class M2MPasswordController {

    private final M2MService m2mService;

    @Operation(
            description = "Erstellen eines neuen M2M Passwortes"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Das erstellte Passwort")
            }
    )
    @PutMapping
    Mono<M2MCredentials> generatePassword() {
        log.debug("generate M2M password...");
        return m2mService.generatePassword();
    }

}
