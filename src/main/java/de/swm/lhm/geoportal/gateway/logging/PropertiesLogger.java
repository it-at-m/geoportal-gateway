package de.swm.lhm.geoportal.gateway.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.Collection;


@Slf4j
@Component
public class PropertiesLogger {

    @EventListener
    public void printProperties(ContextRefreshedEvent contextRefreshedEvent) {
        log.debug(">>> active configuration properties  <<<");

        ConfigurableEnvironment env = ((ConfigurableEnvironment) contextRefreshedEvent.getApplicationContext().getEnvironment());

        env.getPropertySources()
                .stream()
                .filter(propertySource -> !propertySource.getName().startsWith("system"))
                .filter(MapPropertySource.class::isInstance)
                .map(ps -> ((MapPropertySource) ps).getSource().keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(key -> printIt(key, env.getProperty(key)));
    }

    private void printIt(final String key, final String value) {
        String maskedValue = value;
        if (!StringUtils.isBlank(value) && StringUtils.containsAnyIgnoreCase(key, "password", "pwd", "passphrase", "secret"))
            maskedValue = "***hidden***";
        log.debug("{}={}", key, maskedValue);
    }

}

