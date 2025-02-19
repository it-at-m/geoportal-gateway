package de.swm.lhm.geoportal.gateway.contactform;


import de.swm.lhm.geoportal.gateway.contactform.model.ContactFormResponse;
import de.swm.lhm.geoportal.gateway.contactform.model.ContactFormTo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Receives contact form data and initiates mail sending.
 */
@Tag(name = "Kontakt Formular")
@RestController
@RequestMapping(value = "/api/${geoportal.gateway.api.version}/contactform")
@AllArgsConstructor
@Slf4j
public class ContactFormController {

    private final ContactFormService contactFormService;

    /**
     * sends the contactFormTo as EMail
     *
     * @param contactFormTo as {@link ContactFormTo}
     * @return the Response as {@link ContactFormResponse}
     */
    @Operation(
            description = "Kontaktanfrage E-Mail versenden"
    )
    @PostMapping
    public Mono<ContactFormResponse> sendContactFormMail(@RequestBody(required = true, description = "Anfrageobjekt mit Kontaktformulardaten") ContactFormTo contactFormTo) {

        return contactFormService.sendMail(contactFormTo).map(
                recipientMailAddress -> {
                    ContactFormResponse contactFormResponse = new ContactFormResponse();
                    contactFormResponse.setResponseText("Mail send OK");
                    return contactFormResponse;
                }
        );
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ContactFormTransportException.class)
    public void handleContactFormTransportException() {
        log.error("Could not send contact form");
    }

}
