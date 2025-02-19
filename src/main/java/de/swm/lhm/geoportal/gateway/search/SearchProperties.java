package de.swm.lhm.geoportal.gateway.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.search")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchProperties {
    private String endpoint;
}
