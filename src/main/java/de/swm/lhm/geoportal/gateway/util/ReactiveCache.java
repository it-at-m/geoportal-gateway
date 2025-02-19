package de.swm.lhm.geoportal.gateway.util;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ReactiveCache<K, V> {
    AsyncCache<K, V> asyncCache;

    public ReactiveCache(long maximumSize, Duration expireAfterWrite) {
        this.asyncCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite)
                .scheduler(Scheduler.systemScheduler())
                .buildAsync();
    }

    public Mono<V> get(K key, Mono<V> provider) {
        return Mono.fromFuture(asyncCache.get(key, (cacheKey, executor) -> provider.toFuture()));
    }
}
