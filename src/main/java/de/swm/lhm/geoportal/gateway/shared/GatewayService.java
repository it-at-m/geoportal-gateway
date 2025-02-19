package de.swm.lhm.geoportal.gateway.shared;


import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

// https://www.baeldung.com/spring-get-current-applicationcontext

@Service
@RequiredArgsConstructor
public class GatewayService implements ApplicationContextAware {

    @Getter
    private static ApplicationContext applicationContext;

    private final GeoPortalGatewayProperties geoportalGatewayProperties;

    public String getExternalUrl(){
        return geoportalGatewayProperties.getExternalUrlPrepared();
    }

    public Stage getStage(){
        return geoportalGatewayProperties.getStagePrepared();
    }

    @Override
    public synchronized void setApplicationContext(@Nullable ApplicationContext applicationContext) throws BeansException {
        GatewayService.applicationContext = applicationContext;
    }

}