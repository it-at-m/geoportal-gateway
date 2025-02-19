package de.swm.lhm.geoportal.gateway.search.model.elastic;

import lombok.Data;

import java.util.List;

@Data
public class ElasticSearchOuterHits<T> {
    private Integer total;
    private List<ElasticSearchInnerHit<T>> hits;
}
