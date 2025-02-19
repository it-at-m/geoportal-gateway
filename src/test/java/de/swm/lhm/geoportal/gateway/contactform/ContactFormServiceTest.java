package de.swm.lhm.geoportal.gateway.contactform;

import de.swm.lhm.geoportal.gateway.contactform.model.ContactFormTo;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class ContactFormServiceTest {

    private static final String HTML_MESSAGE = """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <title>Simple Transactional Email</title>
                  </head>
                  <body>
                    Test Message
                  </body>
                </html>
                """;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ContactFormProperties contactFormProperties;

    @InjectMocks
    private ContactFormService contactFormService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(contactFormProperties.getSender()).thenReturn("sender@example.com");
        when(contactFormProperties.getRecipient()).thenReturn("recipient@example.com");
    }

    @Test
    void testSendMail() throws MessagingException, IOException {
        ContactFormTo contactFormTo = new ContactFormTo();
        contactFormTo.setSubject("Test Subject");
        contactFormTo.setText(HTML_MESSAGE);
        contactFormTo.setSendUserCopy(false);

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Mono<String> result = contactFormService.sendMail(contactFormTo);

        StepVerifier.create(result)
                .expectNext("recipient@example.com")
                .verifyComplete();

        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(mimeMessageCaptor.capture());

        MimeMessage sentMimeMessage = mimeMessageCaptor.getValue();

        assertEquals("sender@example.com", sentMimeMessage.getFrom()[0].toString());
        assertEquals("recipient@example.com", sentMimeMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Test Subject", sentMimeMessage.getSubject());

        // Überprüfen des Inhalts
        Multipart mp = (Multipart) sentMimeMessage.getContent();
        BodyPart bp = mp.getBodyPart(0);
        assertEquals(HTML_MESSAGE, bp.getContent().toString());

        // Überprüfen des Content-Types
        assertTrue(bp.getContentType().contains("text/html"));
        assertTrue(bp.getContentType().contains("charset=UTF-8"));
    }

    @Test
    void testSendMailWithUserCopy() throws MessagingException, IOException {
        ContactFormTo contactFormTo = new ContactFormTo();
        contactFormTo.setSubject("Test Subject");
        contactFormTo.setText("Test Message");
        contactFormTo.setUserMail("user@example.com");
        contactFormTo.setSendUserCopy(true);

        MimeMessage mimeMessage1 = new MimeMessage((Session) null);
        MimeMessage mimeMessage2 = new MimeMessage((Session) null);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage1).thenReturn(mimeMessage2);

        Mono<String> result = contactFormService.sendMail(contactFormTo);

        StepVerifier.create(result)
                .expectNext("recipient@example.com")
                .verifyComplete();

        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, times(2)).send(mimeMessageCaptor.capture());

        List<MimeMessage> capturedMessages = mimeMessageCaptor.getAllValues();

        MimeMessage sentMimeMessage = capturedMessages.stream()
                .filter(message -> {
                    try {
                        return message.getRecipients(MimeMessage.RecipientType.TO)[0].toString().equals("recipient@example.com");
                    } catch (MessagingException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Original message not found"));

        MimeMessage userCopyMimeMessage = capturedMessages.stream()
                .filter(message -> {
                    try {
                        return message.getRecipients(MimeMessage.RecipientType.TO)[0].toString().equals("user@example.com");
                    } catch (MessagingException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("User copy message not found"));

        assertEquals("sender@example.com", sentMimeMessage.getFrom()[0].toString());
        assertEquals("recipient@example.com", sentMimeMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Test Subject", sentMimeMessage.getSubject());

        Multipart mpOriginal = (Multipart) sentMimeMessage.getContent();
        BodyPart bpOriginal = mpOriginal.getBodyPart(0);
        assertEquals("Test Message", bpOriginal.getContent().toString());
        assertTrue(bpOriginal.getContentType().contains("text/html"));
        assertTrue(bpOriginal.getContentType().contains("charset=UTF-8"));

        // Kopie an Benutzer
        assertEquals("user@example.com", userCopyMimeMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString());

        Multipart mpUserCopy = (Multipart) userCopyMimeMessage.getContent();
        BodyPart bpUserCopy = mpUserCopy.getBodyPart(0);
        assertEquals("Test Message", bpUserCopy.getContent().toString());
        assertTrue(bpUserCopy.getContentType().contains("text/html"));
        assertTrue(bpUserCopy.getContentType().contains("charset=UTF-8"));

    }

    @Test
    void testSendMailThrowsMessagingException() {
        ContactFormTo contactFormTo = new ContactFormTo();
        contactFormTo.setSubject("Test Subject");
        contactFormTo.setText("Test Message");
        contactFormTo.setSendUserCopy(false);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        doAnswer(invocation -> { throw new MessagingException("Failed to send email"); })
                .when(javaMailSender).send(any(MimeMessage.class));

        Mono<String> result = contactFormService.sendMail(contactFormTo);

        StepVerifier.create(result)
                .expectError(ContactFormTransportException.class)
                .verify();

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

}