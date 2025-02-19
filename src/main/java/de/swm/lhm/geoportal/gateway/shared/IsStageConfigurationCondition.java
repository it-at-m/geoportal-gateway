package de.swm.lhm.geoportal.gateway.shared;

import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import jakarta.annotation.Nonnull;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


public class IsStageConfigurationCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {

        GeoPortalGatewayProperties config = Binder.get(context.getEnvironment())
                .bind("geoportal.gateway", GeoPortalGatewayProperties.class)
                .orElse(new GeoPortalGatewayProperties())
                .prepareStage();

        return config.getStagePrepared() != null && Stage.CONFIGURATION.equals(config.getStagePrepared());

    }

}

