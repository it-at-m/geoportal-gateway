package de.swm.lhm.geoportal.gateway.portal;

import de.swm.lhm.geoportal.gateway.shared.BaseEndpointProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.portal")
public class PortalProperties extends BaseEndpointProperties {

}
