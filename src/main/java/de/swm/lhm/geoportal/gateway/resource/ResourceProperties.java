package de.swm.lhm.geoportal.gateway.resource;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "geoportal.resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class ResourceProperties {
    @Pattern(regexp = "^/.*[^/]$", message = "Path must start with / and not end with /")
    private String endpoint;
    private String documentsFolder;
    @NotNull(message = "enableWebserver must have value true or false")
    private Boolean enableWebserver;
    private String webserverPath;
    private String localPath;

}
