package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ReactiveUtilsTest {

    @Test
    void runInBackgroundWithTimeoutTimeouts() {
        String value = ReactiveUtils.runInBackgroundWithTimeout(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(100));
                    } catch (InterruptedException ignored) {
                    }
                    return "value";
                }, Duration.ofMillis(50))
                .onErrorResume(TimeoutException.class, (e) -> Mono.just("timeout"))
                .block();
        assertThat(value, is("timeout"));
    }

    @Test
    void runInBackgroundWithTimeoutWithinAllowedDuration() {
        String value = ReactiveUtils.runInBackgroundWithTimeout(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(10));
                    } catch (InterruptedException ignored) {
                    }
                    return "value";
                }, Duration.ofMillis(500))
                .onErrorResume(TimeoutException.class, (e) -> Mono.just("timeout"))
                .block();
        assertThat(value, is("value"));
    }

    @Test
    void runInBackgroundWithTimeoutEmptyAfterTimeout() {
        String value = ReactiveUtils.runInBackgroundEmptyAfterTimeout(() -> {
                            try {
                                Thread.sleep(Duration.ofMillis(100));
                            } catch (InterruptedException ignored) {
                            }
                            return "value";
                        },
                        Duration.ofMillis(50),
                        () -> {
                        })
                .switchIfEmpty(Mono.just("fallback"))
                .block();
        assertThat(value, is("fallback"));

    }
}
