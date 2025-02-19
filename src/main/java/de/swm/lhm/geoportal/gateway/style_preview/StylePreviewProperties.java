package de.swm.lhm.geoportal.gateway.style_preview;


import de.swm.lhm.geoportal.gateway.shared.BaseEndpointProperties;
import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Conditional(IsStageConfigurationCondition.class)
@Configuration
@ConfigurationProperties(prefix = "geoportal.gateway.style-preview")
@Component
@Data
@EqualsAndHashCode(callSuper = true)
public class StylePreviewProperties extends BaseEndpointProperties {
    public static final String STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME = "style-preview";
}
