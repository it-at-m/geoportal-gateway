package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import reactor.core.publisher.Mono;

interface ReferencedColumnValueTransformer {
    // returning an empty mono indicates the column will be filtered out and
    // omitted from the output.
    Mono<String> accept(ReferencedColumnValue referencedColumnValue);
}
