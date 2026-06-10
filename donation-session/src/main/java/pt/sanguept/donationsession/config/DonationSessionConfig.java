package pt.sanguept.donationsession.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DonationSessionConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
