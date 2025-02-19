package de.swm.lhm.geoportal.gateway.product;

import de.swm.lhm.geoportal.gateway.product.model.Product;
import reactor.core.publisher.Mono;

public interface HasProductDetails {

    Mono<Void> enrichProduct(Product product);

}
