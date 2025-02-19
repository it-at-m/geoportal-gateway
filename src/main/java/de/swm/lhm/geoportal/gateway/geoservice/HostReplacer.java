package de.swm.lhm.geoportal.gateway.geoservice;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HostReplacer {
    private final GeoServiceProperties geoServiceProperties;
    private List<String> privateHostnames;
    private List<String> publicHostnames;

    public HostReplacer(GeoServiceProperties geoServiceProperties) {
        this.geoServiceProperties = geoServiceProperties;
        initHostnameMapping();
    }

    private void initHostnameMapping() {
        privateHostnames = Lists.newArrayList();
        publicHostnames = Lists.newArrayList();
        Iterable<String> mappings = Splitter.on(';').split(geoServiceProperties.getHostnameMapping());
        for (String mapping : mappings) {
            List<String> privateToPublicMapping = Splitter.on(',').splitToList(mapping);
            if (privateToPublicMapping.size() == 2) {
                privateHostnames.add(privateToPublicMapping.get(0));
                publicHostnames.add(privateToPublicMapping.get(1));
            } else {
                log.atError()
                   .setMessage(() -> String.format("Illegal Configuration for private/public Hostname Pair. No Public Hostname found for %s", privateToPublicMapping.getFirst()))
                   .log();
            }
        }
    }

    private String replaceHostNames(String body, List<String> source, List<String> replacements) {
        if (source != null && !source.isEmpty()) {
            String[] sourceHostNames = new String[source.size()];
            sourceHostNames = source.toArray(sourceHostNames);
            String[] replacementHostNames = new String[replacements.size()];
            replacementHostNames = replacements.toArray(replacementHostNames);
            return StringUtils.replaceEach(body, sourceHostNames, replacementHostNames);
        }
        return body;
    }

    public String replaceInternalHostNames(String body) {
        return replaceHostNames(body, this.privateHostnames, this.publicHostnames);
    }

    public String replacePublicHostNames(String body) {
        return replaceHostNames(body, this.publicHostnames, this.privateHostnames);
    }
}
