package de.swm.lhm.geoportal.gateway.search.model.elastic;

import lombok.Data;

@Data
public class ElasticSearchResponse<T> {
    private Integer took;
    private ElasticSearchOuterHits<T> hits;
}

