package pt.sanguept.commonweb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties("rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private List<String> excludePaths = List.of("/actuator", "/actuator/**");
    private BandwidthConfig defaultLimit = new BandwidthConfig();
    private Map<String, BandwidthConfig> paths = new LinkedHashMap<>();

    @Setter
    @Getter
    public static class BandwidthConfig {
        private long capacity = 100;
        private long refillTokens = 100;
        private long refillPeriod = 1;
        private ChronoUnit refillUnit = ChronoUnit.MINUTES;

    }
}
