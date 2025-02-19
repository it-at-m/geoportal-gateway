package de.swm.lhm.geoportal.gateway.style_preview.repository;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreview;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Conditional(IsStageConfigurationCondition.class)
@Repository
public interface StylePreviewRepository extends R2dbcRepository<StylePreview, String> {


}
