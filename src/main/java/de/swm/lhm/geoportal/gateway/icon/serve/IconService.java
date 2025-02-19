package de.swm.lhm.geoportal.gateway.icon.serve;

import de.swm.lhm.geoportal.gateway.icon.IconProperties;
import de.swm.lhm.geoportal.gateway.util.FileServeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IconService {
    private final IconProperties iconProperties;
    public Mono<Resource> getIcon(String relativeFilePath){

        return FileServeUtils.getFileAsResource(iconProperties.getDir(), relativeFilePath);

    }
}
