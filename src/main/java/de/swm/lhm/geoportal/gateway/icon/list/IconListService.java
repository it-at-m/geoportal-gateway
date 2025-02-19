package de.swm.lhm.geoportal.gateway.icon.list;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.annotations.VisibleForTesting;
import de.swm.lhm.geoportal.gateway.icon.IconProperties;
import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import de.swm.lhm.geoportal.gateway.shared.files_search.CachedFilesSearchService;
import de.swm.lhm.geoportal.gateway.shared.files_search.FilesSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;

@Conditional(IsStageConfigurationCondition.class)
@Service
@Slf4j
public class IconListService extends CachedFilesSearchService {
    public static final String CACHE_KEY = "IconServiceFilesList";
    public IconListService(
            FilesSearchService filesSearchService,
            IconProperties iconProperties
    ) {
        super(filesSearchService, iconProperties, CACHE_KEY);
    }

    @VisibleForTesting
    protected Cache<String, List<String>> getCache(){
        return this.filesListCache;
    }
}
