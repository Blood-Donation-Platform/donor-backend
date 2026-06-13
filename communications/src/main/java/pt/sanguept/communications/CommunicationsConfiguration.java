package pt.sanguept.communications;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CommunicationsProperties.class)
public class CommunicationsConfiguration {
}
