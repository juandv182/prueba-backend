package fastglp.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class TestTimezoneConfig {
    @PostConstruct
    public void init(){
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }
}
