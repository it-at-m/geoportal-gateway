package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

@UtilityClass
public class ReactiveUtils {

    public static <T> Mono<T> optionalToMono(Optional<T> value) {
        return value
                .map(Mono::just)
                .orElseGet(Mono::empty);
    }

    /**
     * Runs blocking operations in a background thread
     *
     */
    public static <T> Mono<T> runInBackground(Callable<T> callable) {
        return Mono.fromCallable(callable)
                .subscribeOn(Schedulers.boundedElastic());
    }


    public static <T> Mono<T> runInBackgroundWithTimeout(Callable<T> callable, Duration duration) {
        return runInBackground(callable)
                .timeout(duration);
    }

    /**
     * Runs a blocking operation in a background thread and returns an empty
     * Mono when the callable was not finished within the given duration
     *
     */
    public static <T> Mono<T> runInBackgroundEmptyAfterTimeout(Callable<T> callable, Duration duration, Runnable timeoutAction) {
        return runInBackgroundWithTimeout(callable, duration)
                .onErrorResume(TimeoutException.class, e -> {
                    timeoutAction.run();
                    return Mono.empty();
                });
    }
}
