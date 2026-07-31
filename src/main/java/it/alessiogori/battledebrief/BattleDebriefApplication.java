package it.alessiogori.battledebrief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BattleDebriefApplication {

    public static void main(String[] args) {
        SpringApplication.run(BattleDebriefApplication.class, args);
    }
}
