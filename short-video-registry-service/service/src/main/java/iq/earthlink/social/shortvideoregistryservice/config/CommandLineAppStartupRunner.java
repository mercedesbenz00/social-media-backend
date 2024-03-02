package iq.earthlink.social.shortvideoregistryservice.config;

import iq.earthlink.social.shortvideoregistryservice.repository.migration.SchemaMigrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Used for the schema migrations executed on the Cassandra database at the application startup.
 */
@Profile("!test")
@Component
@Slf4j
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);

    private final SchemaMigrationRepository schemaMigrationRepository;

    public CommandLineAppStartupRunner(SchemaMigrationRepository schemaMigrationRepository) {
        this.schemaMigrationRepository = schemaMigrationRepository;
    }


    @Override
    public void run(String... args) {
        var schemaMigrations = schemaMigrationRepository.findAll();
        LOGGER.info("Listing the schema migrations");
        schemaMigrations.forEach(schemaMigration ->
            LOGGER.info("Schema migration applied: {} version: {} script name: {}",
                    schemaMigration.getSchemaMigrationKey().isAppliedSuccessful(),
                    schemaMigration.getSchemaMigrationKey().getVersion(),
                    schemaMigration.getScriptName()));


    }
}

