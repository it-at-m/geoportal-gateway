package de.swm.lhm.geoportal.gateway.loadbalancer;

import jakarta.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = LoadBalancerProperties.LOAD_BALANCER_PROPERTIES_PREFIX)
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class LoadBalancerProperties {
    public static final String LOAD_BALANCER_PROPERTIES_PREFIX = "geoportal.gateway.load-balancer";

    private static final List<String> TRUE_VALUES = List.of("true", "yes", "ja", "1");
    private static final List<String> FALSE_VALUES = List.of("false", "no", "nein", "0", "-1");

    // services will be loaded via application.properties / yml
    private Map<String, List<Object>> services = new HashMap<>();

    // services will be converted into this internal structure
    private Set<String> serviceIds = new HashSet<>();
    private Map<String, List<LoadBalancerServiceInstance>> servicesMap = new HashMap<>();
    private Map<String, AtomicInteger> servicesCount = new HashMap<>();
    private Map<String, LoadBalancerServiceInstance> instancesMap = new HashMap<>();
    private Map<String, Boolean> stickyMap = new HashMap<>();

    @PostConstruct
    public void init() throws URISyntaxException {

        for (Map.Entry<String, List<Object>> entry : services.entrySet()) {

            String rawKey = entry.getKey();
            List<String> servicePath = Arrays.asList(rawKey.split("\\."));

            if (servicePath.isEmpty() || servicePath.size() == 1) {
                continue;
            }

            String serviceId = servicePath.get(0);

            if (servicePath.get(1).equals("urls")) {
                loadUrls(serviceId, entry.getValue().stream().filter(String.class::isInstance).map(String.class::cast).toList());
            } else if (servicePath.get(1).equals("sticky")) {
                loadSticky(serviceId, servicePath, entry.getValue(), rawKey);
            }

        }

        cleanupSettings();

    }

    private void loadUrls(
            String serviceId,
            List<String> serviceUrls
    ) throws URISyntaxException {

        List<LoadBalancerServiceInstance> servicesList = servicesMap.computeIfAbsent(serviceId, k -> new ArrayList<>());

        for (String serviceUrl : serviceUrls) {

            if (serviceUrl.trim().length() > 1) {
                serviceUrl = serviceUrl.trim().replace(",", ";");
                if (serviceUrl.contains(";")) {
                    loadUrls(serviceId, List.of(serviceUrl.split(";")));
                } else if (StringUtils.isNotBlank(serviceUrl)) {
                    LoadBalancerServiceInstance instance = new LoadBalancerServiceInstance(serviceId, serviceUrl);
                    servicesList.add(instance);
                    instancesMap.put(instance.getInstanceId(), instance);
                }
            }
        }
    }

    private void loadSticky(
            String serviceId,
            List<String> servicePath,
            List<Object> stickySettings,
            String rawKey
    ) {

        if (servicePath.size() == 2 && !stickySettings.isEmpty()) {

            Object rawValue = stickySettings.getFirst();

            Boolean value = null;

            if (rawValue instanceof String stringValue) {
                if (TRUE_VALUES.contains(stringValue.trim().toLowerCase(Locale.ROOT))) value = true;
                else if (FALSE_VALUES.contains(stringValue.trim().toLowerCase(Locale.ROOT))) value = false;
            } else if (rawValue instanceof Boolean boolValue) value = boolValue;

            if (value == null) {
                log.atError()
                        .setMessage(() -> String.format(
                                "Value %s=%s is not a boolean (%s|%s)",
                                rawKey,
                                rawValue,
                                String.join("|", TRUE_VALUES),
                                String.join("|", FALSE_VALUES)))
                        .log();
                value = false;
            }

            if (Boolean.TRUE.equals(value)) {
                stickyMap.put(serviceId, value);
            }

        }
    }

    private void cleanupSettings() {

        ArrayList<String> idsToRemove = new ArrayList<>();

        for (Map.Entry<String, List<LoadBalancerServiceInstance>> entry : servicesMap.entrySet()) {
            String serviceId = entry.getKey();
            List<LoadBalancerServiceInstance> serviceSettings = entry.getValue();
            if (serviceSettings.isEmpty())
                idsToRemove.add(serviceId);
            else {
                serviceIds.add(serviceId);
                servicesCount.put(serviceId, new AtomicInteger());
            }
        }
        idsToRemove.forEach(servicesMap::remove);
        idsToRemove.forEach(stickyMap::remove);
    }

}