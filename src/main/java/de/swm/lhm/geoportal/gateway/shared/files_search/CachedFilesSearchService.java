package de.swm.lhm.geoportal.gateway.shared.files_search;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CachedFilesSearchService {
    private final FilesSearchService filesSearchService;
    private final BaseFileSearchProperties fileSearchProperties;
    protected final Cache<String, List<String>> filesListCache;
    private final String filesKey;

    public CachedFilesSearchService(
            FilesSearchService filesSearchService,
            BaseFileSearchProperties fileSearchProperties,
            String filesKey
    ) {
        this.filesSearchService = filesSearchService;
        this.fileSearchProperties = fileSearchProperties;
        this.filesListCache = initFilesListCache();
        this.filesKey = filesKey;
    }

    private Cache<String, List<String>> initFilesListCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000L)
                .evictionListener((key, item, cause) -> log.trace(this.getClass().getSimpleName().concat(": key {} was evicted "), key))
                .removalListener((key, item, cause) -> log.trace(this.getClass().getSimpleName().concat(": Key {} was removed "), key))
                .scheduler(Scheduler.systemScheduler())
                .build();
    }

    public List<String> getFiles() {
        return filesListCache.get(filesKey, k -> readFileNames());
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @EventListener(ApplicationReadyEvent.class)
    private void renewCache() {
        filesListCache.put(filesKey, readFileNames());
    }

    private List<String> readFileNames() {
        try {
            return this.filesSearchService.getFilenamesFromDirectory(
                    fileSearchProperties.getFileIdentifier(),
                    fileSearchProperties.getDir(),
                    fileSearchProperties.getGlobPattern()
            );
        } catch (IOException | IllegalArgumentException e) {
            log.error("Could not get files", e);
            return new ArrayList<>();
        }
    }

}

