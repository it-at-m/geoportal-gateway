package de.swm.lhm.geoportal.gateway.resource;


import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.resource.model.FileResource;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import de.swm.lhm.geoportal.gateway.util.FileServeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;


@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService implements HasProductDetails {

    private final GatewayService gatewayService;
    private final ResourceRepository resourceRepository;
    private final ResourceProperties resourceProperties;


    @Override
    public Mono<Void> enrichProduct(Product product) {
        return resourceRepository.findFileResourceByProductId(product.getId())
                .doOnNext(this::mapUrl)
                .doOnNext(product::addFileResource)
                .then();
    }

    private void mapUrl(FileResource fileResource) {
        fileResource.setUrl(buildUrl(fileResource));
    }

    public String buildUrl(FileResource fileResource) {
        try {
            return new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(resourceProperties.getEndpoint())
                    .addPath(fileResource.getUnit())
                    .addPath(resourceProperties.getDocumentsFolder())
                    .addPath(fileResource.getName()).toString();
        } catch (NullPointerException | URISyntaxException e) {
            log.atError()
               .setMessage(() -> String.format(
                   "Failed to build url for file resource %s, gateway external url = %s, resource endpoint = %s, resource documents folder = %s",
                   fileResource.getName(),
                   gatewayService.getExternalUrl(),
                   resourceProperties.getEndpoint(),
                   resourceProperties.getDocumentsFolder()))
               .setCause(e)
               .log();
            return "";
        }
    }

    public Mono<Resource> getResource(String relativeFilePath) {
        return FileServeUtils.getFileAsResource(resourceProperties.getLocalPath(), relativeFilePath);
    }
}


