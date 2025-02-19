package de.swm.lhm.geoportal.gateway.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class GatewayInfoContributor implements org.springframework.boot.actuate.info.InfoContributor {

    private final GatewayInfoProperties gatewayInfoProperties;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", gatewayInfoProperties.asMap());
    }

}
