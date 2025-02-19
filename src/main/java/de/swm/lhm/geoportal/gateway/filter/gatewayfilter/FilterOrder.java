package de.swm.lhm.geoportal.gateway.filter.gatewayfilter;

import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;

public enum FilterOrder {
    // Due to the nature of the filter chain, a filter with lower precedence
    // (a lower order in the chain) will execute its “pre” logic in an earlier stage,
    // but it’s “post” implementation will get invoked later.

    RESPONSE_BEFORE_NETTY_WRITE_FILTER(
            // Must be before netty writes the response, otherwise GatewayFilters are not
            // able to modify the response body and are writeBody of the decorated response is
            // not even called.
            //
            // https://stackoverflow.com/a/77825153
            NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1
    ),
    REQUEST_SECOND_FILTER(2);


    private final int orderKey;

    FilterOrder(int orderKey) {
        this.orderKey = orderKey;
    }

    public int getOrder() {
        return orderKey;
    }
}
