package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import com.google.common.base.Splitter;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * resolves the root layer from a layer name fragment as it is used in GFI responses
 */
class LayerNameResolver {

    private static final Pattern PATTERN_FEATURE_ID_SPLIT = java.util.regex.Pattern.compile("\\.");
    // maps non-workspace-qualified layernames to workspace-qualified layernames
    private final Map<String, HashSet<QualifiedLayerName>> lookupMap = new HashMap<>();

    // maps qualified layer names to their root laywe
    private final Map<QualifiedLayerName, QualifiedLayerName> rootLayerMap = new HashMap<>();


    public void add(QualifiedLayerName rootLayerName, Collection<QualifiedLayerName> childLayerNames) {
        childLayerNames.forEach(childLayerName -> this.add(rootLayerName, childLayerName));
    }

    public void add(QualifiedLayerName rootQualifiedLayerName, QualifiedLayerName qLayerName) {
        HashSet<QualifiedLayerName> found = lookupMap.getOrDefault(qLayerName.layerName(), new HashSet<>());
        found.add(qLayerName);
        lookupMap.put(qLayerName.layerName(), found);

        rootLayerMap.put(qLayerName, rootQualifiedLayerName);
    }

    public void add(QualifiedLayerName qLayerName) {
        this.add(qLayerName, qLayerName);
    }

    public Optional<QualifiedLayerName> getRootLayer(QualifiedLayerName qLayerName) {
        return Optional.ofNullable(rootLayerMap.get(qLayerName));
    }

    public Optional<QualifiedLayerName> resolveLayerNameFromFeatureId(String featureId) {
        // Feature-IDs are in the form "<layername-without-workspace>.<id of table>"
        // Example: "gis_osm_pois_free_1_o2o.77462"
        List<String> featureIdParts = Splitter.on(PATTERN_FEATURE_ID_SPLIT).splitToList(featureId);
        if (featureIdParts.size() < 1) {
            return Optional.empty();
        }
        return resolveFromNonQualifiedLayerName(featureIdParts.get(0));
    }

    public Optional<QualifiedLayerName> resolveFromNonQualifiedLayerName(String layerName) {
        Set<QualifiedLayerName> variants = lookupMap.get(layerName.toLowerCase(Locale.ROOT).strip());
        if (variants == null) {
            return Optional.empty();
        }
        return variants.stream().findFirst();
    }

    public boolean contains(QualifiedLayerName qLayerName) {
        Set<QualifiedLayerName> variants = lookupMap.get(qLayerName.layerName());
        return variants != null && !variants.isEmpty() &&  variants.contains(qLayerName);
    }
}
