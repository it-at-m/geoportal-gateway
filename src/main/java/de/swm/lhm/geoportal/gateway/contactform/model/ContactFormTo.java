
package de.swm.lhm.geoportal.gateway.contactform.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Contains data from the contact form of the map client.
 */

@Schema(
        description = "Kontaktformular Anfrageobjekt"
)
@Data
public class ContactFormTo {

    @Schema(
            description = "Thema"
    )
    private String subject;

    @Schema(
            description = "Text"
    )
    private String text;

    @Schema(
            description = "Soll eine Kopie an den User gesendet werden?"
    )
    private boolean sendUserCopy;
    @Schema(
            description = "Email an den User"
    )
    private String userMail;

}
