package de.swm.lhm.geoportal.gateway.sensor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.sensor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SensorLayerProperties {

    private String endpoint;

    private boolean reescapeUrlParameterAscii = true;

    private boolean replaceOriginatingUrl = false;

}
