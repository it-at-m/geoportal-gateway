package de.swm.lhm.geoportal.gateway.actuator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "git")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class GatewayGitProperties {

    private GitBuild build;
    private GitCommit commit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitBuild {
        private String version;
        private String time;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitCommit {
        private GitId id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitId {
        private String full;
        private String abbrev;
    }

    public Map<String, String> asMap() {
        return Map.of("version", build == null ? "" : build.getVersion(), "time", build == null ? "" : build.getTime(), "id", commit == null || commit.getId() == null || commit.getId().getAbbrev() == null ? "" : commit.getId().getAbbrev());
    }

}