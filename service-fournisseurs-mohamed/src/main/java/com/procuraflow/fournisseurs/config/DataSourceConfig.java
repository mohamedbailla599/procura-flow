package com.procuraflow.fournisseurs.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(
        name = "app.datasource.neon-resolver-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${app.datasource.raw-url:}") String rawUrl,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${app.datasource.schema:schema_fournisseurs}") String schema,
            @Value("${spring.datasource.hikari.maximum-pool-size:5}") int maxPoolSize,
            @Value("${spring.datasource.hikari.minimum-idle:1}") int minIdle,
            @Value("${spring.datasource.hikari.connection-timeout:10000}") long connectionTimeoutMs,
            @Value("${spring.datasource.hikari.idle-timeout:300000}") long idleTimeoutMs,
            @Value("${spring.datasource.hikari.max-lifetime:600000}") long maxLifetimeMs
    ) {
        NeonJdbcUrlResolver.ResolvedJdbcConfig resolved = NeonJdbcUrlResolver.resolve(
                rawUrl,
                username,
                password,
                schema
        );

        log.info("Configuring PostgreSQL DataSource for schema '{}'", schema);

        HikariConfig config = new HikariConfig();
        config.setPoolName("FournisseursHikariPool");
        config.setJdbcUrl(resolved.jdbcUrl());
        if (StringUtils.hasText(resolved.username())) {
            config.setUsername(resolved.username());
        }
        if (StringUtils.hasText(resolved.password())) {
            config.setPassword(resolved.password());
        }
        config.setSchema(schema);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(idleTimeoutMs);
        config.setMaxLifetime(maxLifetimeMs);

        return new HikariDataSource(config);
    }
}
