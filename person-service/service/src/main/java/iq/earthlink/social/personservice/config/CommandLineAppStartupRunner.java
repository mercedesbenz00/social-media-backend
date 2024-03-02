package iq.earthlink.social.personservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommandLineAppStartupRunner implements CommandLineRunner {


    @Override
    public void run(String... args) {
        log.info("Running app startup configurations");
    }
}
