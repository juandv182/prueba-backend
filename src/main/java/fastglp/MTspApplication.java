package fastglp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
//@EnableScheduling
public class MTspApplication {
    public static void main(String[] args) {
        SpringApplication.run(MTspApplication.class, args);
    }
}