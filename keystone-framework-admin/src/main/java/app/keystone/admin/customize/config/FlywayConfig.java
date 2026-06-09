package app.keystone.admin.customize.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner flywayMigrationRunner(DataSource dataSource, FlywayProperties properties) {
        return args -> {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .validateOnMigrate(properties.isValidateOnMigrate())
                .outOfOrder(properties.isOutOfOrder())
                .locations(properties.getLocations().toArray(String[]::new))
                .load();
            log.info("Running Flyway migrations with locations {}", properties.getLocations());
            flyway.migrate();
        };
    }
}
