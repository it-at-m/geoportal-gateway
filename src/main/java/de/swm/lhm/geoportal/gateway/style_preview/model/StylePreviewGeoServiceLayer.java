package de.swm.lhm.geoportal.gateway.style_preview.model;

import com.google.common.base.Splitter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylePreviewGeoServiceLayer {

    @Id
    @Column("id")
    String stylePreviewId;

    @Column("name")
    String layerName;

    @Column("workspace")
    String layerWorkspace;

    public static class StylePreviewGeoServiceLayerBuilder {
        public StylePreviewGeoServiceLayerBuilder workspaceAndName(String workspaceAndName) {
            List<String> workspaceAndNameArray = Splitter.on(':').splitToList(workspaceAndName);
            assert workspaceAndNameArray.size() == 2;
            this.layerName = workspaceAndNameArray.get(1);
            this.layerWorkspace = workspaceAndNameArray.get(0);
            return this;
        }
    }
}
