package de.swm.lhm.geoportal.gateway.geoservice;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "geoportal.geoserver")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoServiceProperties {

    private String url;
    private String endpoint;
    private String hostnameMapping;
    private Set<String> blockedRequestTypes;
    private int maxXmlParsingDurationMs;

    @PostConstruct
    private void init() {
        if (blockedRequestTypes == null) {
            this.blockedRequestTypes = Collections.emptySet();
        } else {
            this.blockedRequestTypes = this.blockedRequestTypes.stream()
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
    }

    public int getSanitizedMaxXmlParsingDurationMs() {
        return Math.max(0, maxXmlParsingDurationMs);
    }
}
