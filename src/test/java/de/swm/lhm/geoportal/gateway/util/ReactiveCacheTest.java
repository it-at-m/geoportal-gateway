package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveCacheTest {

    @Test
    void cacheGet() {
        ReactiveCache<String, String> cache = new ReactiveCache<>(10, Duration.ofSeconds(10));
        AtomicInteger counter = new AtomicInteger();
        counter.set(0);

        for (int i = 0; i < 10; i++) {
            String value = cache.get("key", Mono.fromCallable(() -> {
                        counter.incrementAndGet();
                        return "value";
                    }))
                    .block();
            assertThat(value).isEqualTo("value");
        }
        assertThat(counter.get()).isEqualTo(1);
    }
}
