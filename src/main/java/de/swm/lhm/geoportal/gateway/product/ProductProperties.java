package de.swm.lhm.geoportal.gateway.product;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductProperties {

    private String endpoint;
    private String imagePath;
}
