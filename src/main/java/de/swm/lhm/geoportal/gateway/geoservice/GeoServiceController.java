package de.swm.lhm.geoportal.gateway.geoservice;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Geodaten-Dienst")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/${geoportal.gateway.api.version}/geoservice")
@Slf4j
public class GeoServiceController {

}

