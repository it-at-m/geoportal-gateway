package de.swm.lhm.geoportal.gateway.contactform.controller;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.ServerSetup;
import de.swm.lhm.geoportal.gateway.contactform.ContactFormController;
import de.swm.lhm.geoportal.gateway.contactform.ContactFormProperties;
import de.swm.lhm.geoportal.gateway.contactform.ContactFormService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login;

@ExtendWith(SpringExtension.class)
@WebFluxTest(
        controllers = ContactFormController.class,
        excludeAutoConfiguration = {
                ReactiveOAuth2ClientAutoConfiguration.class,
                ReactiveOAuth2ResourceServerAutoConfiguration.class
        }
)
@Import({TestSecurityConfig.class, JavaMailSenderImpl.class, ContactFormService.class, ContactFormProperties.class,})
@TestPropertySource("classpath:loadbalancer/load-balancer-properties-test.properties")
@TestPropertySource(
        properties = {
                "spring.mail.port=3025",
                "geoportal.contactform.sender=unittest@example.com",
                "geoportal.contactform.recipient=contact@example.com"
        }
)
class ContactFormControllerComponentTest {

    private static final ServerSetup serverSetup = new ServerSetup(
            3025,
            null,
            ServerSetup.PROTOCOL_SMTP
    )
            .setVerbose(true);
    @RegisterExtension
    protected static final GreenMailExtension greenMail = new GreenMailExtension(serverSetup)
            .withConfiguration(new GreenMailConfiguration())
            .withPerMethodLifecycle(false);
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private JavaMailSender mailSender;

    @Test
    void sendContactFormMail_success() {

        ((JavaMailSenderImpl) mailSender).setPort(3025);

        webClient
                .mutateWith(csrf())
                .mutateWith(mockOAuth2Login())
                .post()
                .uri("/api/v1/contactform")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("subject", "test-subject").with("text", "test-text"))
                .exchange()
                .expectStatus().isOk();

        greenMail.waitForIncomingEmail(5000, 1);
    }
}
