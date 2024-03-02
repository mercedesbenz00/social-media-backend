package iq.earthlink.social.personservice.util;

import iq.earthlink.social.classes.enumeration.EnvironmentType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class EnvironmentUtil {

    private final Environment environment;

    public EnvironmentUtil(Environment environment) {
        this.environment = environment;
    }

    public boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(env -> env.equalsIgnoreCase(EnvironmentType.PRODUCTION.name()));
    }
}
