package de.swm.lhm.geoportal.gateway.contactform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "geoportal.contactform")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ContactFormProperties {

    private String sender;
    private String recipient;

}
