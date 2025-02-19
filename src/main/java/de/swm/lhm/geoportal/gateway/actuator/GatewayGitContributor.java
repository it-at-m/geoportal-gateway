package de.swm.lhm.geoportal.gateway.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class GatewayGitContributor implements org.springframework.boot.actuate.info.InfoContributor {

    private final GatewayGitProperties gatewayGitProperties;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("git", gatewayGitProperties.asMap());
    }

}
