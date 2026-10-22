package com.procuraflow.fournisseurs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NeonJdbcUrlResolverTest {

    @Test
    void shouldConvertLibpqPostgresqlUriWithEmbeddedCredentials() {
        String raw = "postgresql://neon_user:secretPass123@ep-cool-darkness-123456.eu-central-1.aws.neon.tech/procura_flow?sslmode=require";

        NeonJdbcUrlResolver.ResolvedJdbcConfig resolved = NeonJdbcUrlResolver.resolve(
                raw,
                "",
                "",
                "schema_fournisseurs"
        );

        assertThat(resolved.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://ep-cool-darkness-123456.eu-central-1.aws.neon.tech:5432/procura_flow?sslmode=require&currentSchema=schema_fournisseurs"
        );
        assertThat(resolved.username()).isEqualTo("neon_user");
        assertThat(resolved.password()).isEqualTo("secretPass123");
    }

    @Test
    void shouldConvertShortPostgresUriAndPreferExplicitEnvCredentials() {
        String raw = "postgres://embedded_user:embedded_pass@ep-neon.aws.neon.tech:5432/procura_flow?sslmode=require";

        NeonJdbcUrlResolver.ResolvedJdbcConfig resolved = NeonJdbcUrlResolver.resolve(
                raw,
                "explicit_user",
                "explicit_pass",
                "schema_fournisseurs"
        );

        assertThat(resolved.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://ep-neon.aws.neon.tech:5432/procura_flow?sslmode=require&currentSchema=schema_fournisseurs"
        );
        assertThat(resolved.username()).isEqualTo("explicit_user");
        assertThat(resolved.password()).isEqualTo("explicit_pass");
    }

    @Test
    void shouldAppendCurrentSchemaToStandardJdbcUrlWhenMissing() {
        String raw = "jdbc:postgresql://ep-neon.aws.neon.tech:5432/procura_flow?sslmode=require";

        NeonJdbcUrlResolver.ResolvedJdbcConfig resolved = NeonJdbcUrlResolver.resolve(
                raw,
                "db_user",
                "db_pass",
                "schema_fournisseurs"
        );

        assertThat(resolved.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://ep-neon.aws.neon.tech:5432/procura_flow?sslmode=require&currentSchema=schema_fournisseurs"
        );
        assertThat(resolved.username()).isEqualTo("db_user");
        assertThat(resolved.password()).isEqualTo("db_pass");
    }

    @Test
    void shouldPreserveExistingCurrentSchemaInJdbcUrl() {
        String raw = "jdbc:postgresql://postgres-db:5432/procura_flow?currentSchema=schema_fournisseurs";

        NeonJdbcUrlResolver.ResolvedJdbcConfig resolved = NeonJdbcUrlResolver.resolve(
                raw,
                "admin",
                "pass",
                "schema_fournisseurs"
        );

        assertThat(resolved.jdbcUrl()).isEqualTo(raw);
    }

    @Test
    void shouldFailFastWithClearMessageWhenUrlIsMissing() {
        assertThatThrownBy(() -> NeonJdbcUrlResolver.resolve("   ", "user", "pass", "schema_fournisseurs"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    void shouldFailWithSafeMessageWithoutLeakingCredentialsOnUnsupportedScheme() {
        assertThatThrownBy(() -> NeonJdbcUrlResolver.resolve("mysql://user:superSecret@localhost/db", "", "", "schema_fournisseurs"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported DATABASE_URL format")
                .hasMessageNotContaining("superSecret");
    }
}
