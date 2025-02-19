package de.swm.lhm.geoportal.gateway.shared;


import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import jakarta.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Configuration
@ConfigurationProperties(prefix = GeoPortalGatewayProperties.CONFIG_PREFIX)
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Component
public class GeoPortalGatewayProperties {

    public static final String CONFIG_PREFIX = "geoportal.gateway";

    String stage;
    @Getter
    Stage stagePrepared;

    String externalUrl;
    @Getter
    String externalUrlPrepared;

    @Getter
    DataSize maxRequestBodySize = DataSize.ofBytes(0);

    @PostConstruct
    public void postConstruct() throws URISyntaxException {

        prepareStage();
        prepareExternalUrl();

    }

    public GeoPortalGatewayProperties prepareStage(){

        if (isBlank(stage)) {
            log.warn("No stage is configured via {}.stage, setting stage to CONFIGURATION", CONFIG_PREFIX);
            stage = "CONFIGURATION";
        }

        else if (stage.trim().equalsIgnoreCase("config")) {
            stage = "CONFIGURATION";
        }

        try {
            stagePrepared = Stage.valueOf(stage.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException(
                    String.format(
                            "Configured stage %s is not a valid stage, valid stages are %s",
                            stage,
                            Stage.realStages()
                                    .stream()
                                    .map(Stage::toString)
                                    .collect(Collectors.joining(", "))
                    )
            );
        }

        log.debug("stagePrepared={}", stagePrepared);

        return this;
    }

    private void prepareExternalUrl() throws URISyntaxException {

        if (isBlank(externalUrl))
            externalUrl = "";

        ExtendedURIBuilder url = new ExtendedURIBuilder(externalUrl);

        if (isBlank(url.getScheme()))
            url.setScheme("http");

        // if externalUrl=localhost
        // localhost is interpreted as path
        if (isBlank(url.getHost()) && url.getPathSegments().size() == 1) {
            url.setHost(url.getPathSegments().getFirst());
            url.setPathSegments(new ArrayList<>());
        }

        if (isBlank(url.getHost())) {
            url.setHost("localhost");
        }

        externalUrlPrepared = url.toString();
        log.debug("externalUrlPrepared={}", externalUrlPrepared);

    }

    private boolean isBlank(String value){
        return value == null || StringUtils.isBlank(value) || StringUtils.isBlank(value.trim());
    }

}