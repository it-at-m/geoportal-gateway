package de.swm.lhm.geoportal.gateway.product;


import de.swm.lhm.geoportal.gateway.product.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Tag(name = "Produkte")
@RequiredArgsConstructor
@RestController
@RequestMapping("${geoportal.gateway.product.endpoint}")
@Slf4j
public class ProductController {

    private final ProductService productService;

    @Operation(
            description = "Alle Produkte anfragen",
            responses = {@ApiResponse(description = "Produkte")}
    )
    @GetMapping()
    public Flux<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @Operation(
            description = "Logo eines Produktes anfragen",
            responses = {@ApiResponse(description = "Produktresource")}
    )
    @GetMapping("{productName}/${geoportal.gateway.product.image-path}")
    public Mono<Resource> getProductLogo(@RequestBody(required = true, description = "Produktname") @PathVariable("productName") String productName) {

        return productService.getProductImageByProductName(productName)
                .map(productImage -> new ByteArrayResource(productImage.getBytes(), productImage.getName()));

    }

}

