package de.swm.lhm.geoportal.gateway.actuator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class GatewayInfoProperties {

    private String name;
    private String id;

    public Map<String , String> asMap(){
        return Map.of(
                "name", name,
                "id", id
        );
    }

}