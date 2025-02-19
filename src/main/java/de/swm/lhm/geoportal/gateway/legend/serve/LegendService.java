package de.swm.lhm.geoportal.gateway.legend.serve;

import de.swm.lhm.geoportal.gateway.legend.LegendProperties;
import de.swm.lhm.geoportal.gateway.util.FileServeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LegendService {
    private final LegendProperties legendProperties;
    public Mono<Resource> getLegend(String relativeFilePath){

        return FileServeUtils.getFileAsResource(legendProperties.getDir(), relativeFilePath);

    }
}
