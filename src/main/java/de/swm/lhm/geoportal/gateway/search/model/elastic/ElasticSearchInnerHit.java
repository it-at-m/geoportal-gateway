package de.swm.lhm.geoportal.gateway.search.model.elastic;

import lombok.Data;

@SuppressWarnings({"checkstyle:MemberName", "PMD"})
@Data
public class ElasticSearchInnerHit<T> {
    private int _score;
    private T _source;

}
