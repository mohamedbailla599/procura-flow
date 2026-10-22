package com.procuraflow.fournisseurs.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;

/**
 * Resolves and normalizes PostgreSQL / Neon connection strings into valid JDBC URLs
 * without leaking credentials in error messages or logs.
 */
public final class NeonJdbcUrlResolver {

    private NeonJdbcUrlResolver() {
    }

    public record ResolvedJdbcConfig(
            String jdbcUrl,
            String username,
            String password
    ) {
    }

    public static ResolvedJdbcConfig resolve(
            String rawUrl,
            String configuredUsername,
            String configuredPassword,
            String defaultSchema
    ) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalStateException(
                    "Missing required database configuration: set DATABASE_URL environment variable "
                            + "to your Neon PostgreSQL connection string."
            );
        }

        String trimmedUrl = rawUrl.trim();
        String schema = StringUtils.hasText(defaultSchema) ? defaultSchema.trim() : "schema_fournisseurs";

        if (trimmedUrl.startsWith("jdbc:postgresql://") || trimmedUrl.startsWith("jdbc:h2:")) {
            String urlWithSchema = ensureCurrentSchema(trimmedUrl, schema);
            return new ResolvedJdbcConfig(
                    urlWithSchema,
                    trimToNull(configuredUsername),
                    trimToNull(configuredPassword)
            );
        }

        if (trimmedUrl.startsWith("postgresql://") || trimmedUrl.startsWith("postgres://")) {
            return parseLibpqUri(trimmedUrl, configuredUsername, configuredPassword, schema);
        }

        throw new IllegalStateException(
                "Unsupported DATABASE_URL format. Expected 'jdbc:postgresql://...' or 'postgresql://...'."
        );
    }

    private static ResolvedJdbcConfig parseLibpqUri(
            String rawUri,
            String configuredUsername,
            String configuredPassword,
            String schema
    ) {
        String normalizedUri = rawUri.startsWith("postgres://")
                ? "postgresql://" + rawUri.substring("postgres://".length())
                : rawUri;

        URI uri;
        try {
            uri = URI.create(normalizedUri);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid PostgreSQL URI format in DATABASE_URL. Verify host, port, and database name."
            );
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException(
                    "Invalid PostgreSQL URI in DATABASE_URL: missing database host."
            );
        }

        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath();
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            throw new IllegalStateException(
                    "Invalid PostgreSQL URI in DATABASE_URL: missing database name (expected e.g. /procura_flow)."
            );
        }

        String extractedUser = null;
        String extractedPass = null;
        String rawUserInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(rawUserInfo)) {
            int colonIdx = rawUserInfo.indexOf(':');
            if (colonIdx >= 0) {
                extractedUser = urlDecode(rawUserInfo.substring(0, colonIdx));
                extractedPass = urlDecode(rawUserInfo.substring(colonIdx + 1));
            } else {
                extractedUser = urlDecode(rawUserInfo);
            }
        }

        String finalUser = StringUtils.hasText(configuredUsername)
                ? configuredUsername.trim()
                : extractedUser;
        String finalPass = StringUtils.hasText(configuredPassword)
                ? configuredPassword.trim()
                : extractedPass;

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(host)
                .append(':')
                .append(port)
                .append(path);

        String rawQuery = uri.getRawQuery();
        if (StringUtils.hasText(rawQuery)) {
            jdbc.append('?').append(rawQuery);
        }

        String finalJdbcUrl = ensureCurrentSchema(jdbc.toString(), schema);
        return new ResolvedJdbcConfig(finalJdbcUrl, finalUser, finalPass);
    }

    static String ensureCurrentSchema(String jdbcUrl, String schema) {
        if (!jdbcUrl.startsWith("jdbc:postgresql://") || !StringUtils.hasText(schema)) {
            return jdbcUrl;
        }
        if (jdbcUrl.contains("currentSchema=")) {
            return jdbcUrl;
        }
        char separator = jdbcUrl.contains("?") ? '&' : '?';
        return jdbcUrl + separator + "currentSchema=" + schema;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
