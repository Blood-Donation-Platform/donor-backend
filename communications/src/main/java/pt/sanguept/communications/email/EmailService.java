package pt.sanguept.communications.email;

import java.util.Map;

public interface EmailService {

    void send(String to, String subject, String body);

    void sendTemplate(String to, String templateName, Map<String, Object> variables);

}
