package de.swm.lhm.geoportal.gateway.product;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.product.model.ProductImage;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.unit.UnitService;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.List;


@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductProperties productProperties;
    private final UnitService unitService;
    private final IAuthService authorizationService;
    private final GatewayService gatewayService;
    private final Flux<HasProductDetails> productDetailsModules;


    public ProductService(
            ProductRepository productRepository,
            ProductProperties productProperties,
            IAuthService authorizationService,
            List<HasProductDetails> productDetailsModules,
            GatewayService gatewayService,
            UnitService unitService
    ) {
        this.productRepository = productRepository;
        this.productProperties = productProperties;
        this.authorizationService = authorizationService;
        this.productDetailsModules = Flux.fromIterable(productDetailsModules);
        this.gatewayService = gatewayService;
        this.unitService = unitService;
    }

    public Flux<Product> getAllProducts() {
        return authorizationService.getGrantedAsAuthorizationGroup()
                .flatMapMany(authorizationGroup -> productRepository.findByStageAndAuthorizationGroup(gatewayService.getStage(), authorizationGroup))
                .flatMap(product -> enrichProductDetails(product).thenReturn(product))
                .flatMap(this::enrichProduct);
    }

    private Mono<Void> enrichProductDetails(Product product) {
        return productDetailsModules.flatMap(detailsModule -> detailsModule.enrichProduct(product)).then();
    }

    public Mono<ProductImage> getProductImageByProductName(String productName) {

        return authorizationService.getGrantedAsAuthorizationGroup()
                .flatMap(authorizationGroup ->  productRepository.findByNameAndStageAndAuthorizationGroup(productName, gatewayService.getStage(), authorizationGroup))
                .map(product -> ProductImage.builder().name(product.getHeaderImageFileName()).bytes(product.getHeaderImageBytes()).build());
    }

    private Mono<Product> enrichProduct(Product product) {
         return unitService.getUnitNameById(product.getUnitId())
                 .doOnNext(product::setUnit)
                 .thenReturn(product)
                 .doOnNext(this::mapLogoUrl)
                 .thenReturn(product);
    }

    private void mapLogoUrl(Product product) {
        product.setLogoUrl(buildLogoUrl(product));
    }

    private String buildLogoUrl(Product product) {

        try {
            return new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(this.productProperties.getEndpoint())
                    .addPath(product.getName())
                    .addPath(this.productProperties.getImagePath())
                    .toString();
        } catch (URISyntaxException e) {
            log.atError()
               .setMessage(() -> String.format("Failed to build url for product image, productId = %s", product.getId()))
               .setCause(e)
               .log();
            return "";
        }
    }
}


