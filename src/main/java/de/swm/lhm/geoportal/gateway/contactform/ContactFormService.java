package de.swm.lhm.geoportal.gateway.contactform;

import de.swm.lhm.geoportal.gateway.contactform.model.ContactFormTo;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Service implementation of {@link ContactFormService} interface.
 */
@Service
@AllArgsConstructor
public class ContactFormService {

    private static final String MAIL_BODY_CONTENT_TYPE = "text/html; charset=UTF-8";
    private final JavaMailSender javaMailSender;
    private final ContactFormProperties contactFormProperties;

    public Mono<String> sendMail(ContactFormTo contactFormTo) {
        return Mono.fromCallable(() -> {
            try {
                sendMail(contactFormProperties.getRecipient(), contactFormTo);
                if (contactFormTo.isSendUserCopy()) {
                    sendMail(contactFormTo.getUserMail(), contactFormTo);
                }
                return contactFormProperties.getRecipient();
            } catch (MessagingException e) {
                throw new ContactFormTransportException("Error while sending contact form mail occurred", e);
            }
        });
    }

    private void sendMail(String recipient, ContactFormTo contactFormTo) throws MessagingException {

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
        helper.setFrom(contactFormProperties.getSender());
        helper.setTo(recipient);
        helper.setSubject(contactFormTo.getSubject());

        // Erstellen des Inhalts der Nachricht als HTML
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(contactFormTo.getText(), MAIL_BODY_CONTENT_TYPE);

        // Erstellen eines Multipart und Hinzufügen des Nachrichtenteils
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // Setzen des Multipart-Inhalts in die MimeMessage
        mimeMessage.setContent(multipart);
        mimeMessage.saveChanges(); // Speichert die Änderungen und aktualisiert die Header

        javaMailSender.send(mimeMessage);
    }

}