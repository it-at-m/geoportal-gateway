package de.swm.lhm.geoportal.gateway.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class LoadBalancerReactiveClient implements ReactiveDiscoveryClient {

    private final LoadBalancerProperties loadBalancerProperties;

    public LoadBalancerReactiveClient(
            LoadBalancerProperties loadBalancerProperties
    ) {
        this.loadBalancerProperties = loadBalancerProperties;
    }

    @Override
    public String description() {
        return "GeoPortal Gateway Reactive Load Balancer Client";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {

        return Flux.fromIterable(this.loadBalancerProperties.getServicesMap()
                .getOrDefault(serviceId, new ArrayList<>()));
    }

    @Override
    public Flux<String> getServices() {
        return Flux.fromIterable(
                this.loadBalancerProperties.getServiceIds()
        );
    }

    public boolean isSticky(String serviceId) {
        return this.loadBalancerProperties.getStickyMap().containsKey(serviceId)
                && getSticky(serviceId);
    }

    public boolean getSticky(String serviceId) {
        return this.loadBalancerProperties.getStickyMap().get(serviceId);
    }

    public LoadBalancerServiceInstance chooseInstance(WebSession webSession, String serviceId) {

        String instanceId = (String) webSession.getAttributes().get(serviceId);

        if (this.loadBalancerProperties.getInstancesMap().containsKey(instanceId)) {
            return setInstance(webSession, this.loadBalancerProperties.getInstancesMap().get(instanceId));
        }

        List<LoadBalancerServiceInstance> instances = this.loadBalancerProperties.getServicesMap()
                .getOrDefault(serviceId, List.of());

        if (instances.isEmpty()){
            throw new LoadBalancerException("failed to choose a serviceInstance for service id " + serviceId);
        }
        else if (instances.size() == 1) {
            return setInstance(webSession,  instances.getFirst());
        }
        else {
            AtomicInteger counter = this.loadBalancerProperties.getServicesCount().get(serviceId);
            int pos = counter.incrementAndGet() & Integer.MAX_VALUE;
            return setInstance(webSession, instances.get(pos % instances.size()));
        }

    }

    public LoadBalancerServiceInstance setInstance(WebSession webSession, LoadBalancerServiceInstance instance){
        webSession.getAttributes().put(instance.getServiceId(), instance.getInstanceId());
        return instance;
    }


}

