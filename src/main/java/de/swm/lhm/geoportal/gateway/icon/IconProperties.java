package de.swm.lhm.geoportal.gateway.icon;

import de.swm.lhm.geoportal.gateway.shared.files_search.BaseFileSearchProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Component
@Configuration
@ConfigurationProperties(prefix = "geoportal.style.icon")
@Getter
@Setter
@NoArgsConstructor
public class IconProperties extends BaseFileSearchProperties {
    private String endpoint;
}
