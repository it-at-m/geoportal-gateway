package de.swm.lhm.geoportal.gateway.m2m;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "geoportal.m2m")
@Getter
@Setter
@Component
public class M2MProperties {
    String passwordEndpoint;
    String eaiUrl;
}
