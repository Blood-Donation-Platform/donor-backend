package pt.sanguept.communications;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.communications")
public record CommunicationsProperties(
        boolean enabled,
        EmailConfig email
) {

    public record EmailConfig(boolean enabled, String from) {
    }

}
