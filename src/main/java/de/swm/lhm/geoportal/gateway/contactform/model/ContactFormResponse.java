package de.swm.lhm.geoportal.gateway.contactform.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Response type for ContactForm
 */

@Schema(name = "Kontaktanfrage Rückgabeobjekt")
@Data
public class ContactFormResponse {

    @Schema(description = "Rückgabe Text")
    private String responseText;
}
