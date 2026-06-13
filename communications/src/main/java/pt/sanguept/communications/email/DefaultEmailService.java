package pt.sanguept.communications.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pt.sanguept.communications.CommunicationsProperties;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.communications.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final CommunicationsProperties properties;

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.email().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendTemplate(String to, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);

        String fullTemplateName = "email/" + templateName;
        String subject = resolveSubjectFromTemplate(templateName, context);
        String body = templateEngine.process(fullTemplateName, context);

        send(to, subject, body);
    }

    private String resolveSubjectFromTemplate(String templateName, Context context) {
        try {
            return templateEngine.process("email/" + templateName + "-subject", context).trim();
        } catch (Exception e) {
            log.debug("No subject template for '{}', using short template name as subject fallback", templateName);
            return templateName;
        }
    }

}
