package de.swm.lhm.geoportal.gateway.metadata;


import de.swm.lhm.geoportal.gateway.metadata.model.Metadata;
import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;


@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataService implements HasProductDetails {

    private final MetadataRepository metadataRepository;
    private final MetadataProperties metadataProperties;


    @Override
    public Mono<Void> enrichProduct(Product product) {
        return metadataRepository.findMetadataById(product.getMetadataId())
                .map(this::buildUrl)
                .doOnNext(product::setMetadataUrl)
                .then();
    }

    private String buildUrl(Metadata metadata) {

        try {
            return new ExtendedURIBuilder(metadataProperties.getDetailUrl())
                    .addParameter(metadataProperties.getIdParameter(), metadata.getMetadataUuid())
                    .toString();
        } catch (URISyntaxException | NullPointerException e) {
            log.atError()
               .setMessage(() -> String.format(
                   "Failed to build url for metadata %s, metadataDetailUrl=%s, metadataIdParameter=%s",
                   metadata.getMetadataUuid(),
                   metadataProperties.getDetailUrl(),
                   metadataProperties.getIdParameter()))
               .setCause(e)
               .log();
            return "";
        }

    }

}


