package de.swm.lhm.geoportal.gateway.metadata;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetadataProperties {

    private String detailUrl;

    private String idParameter;
}
