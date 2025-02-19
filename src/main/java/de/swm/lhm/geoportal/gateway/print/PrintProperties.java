package de.swm.lhm.geoportal.gateway.print;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geoportal.print")
@Getter
@Setter
@NoArgsConstructor
public class PrintProperties {
    String endpoint;
}
