package com.dataliquid.passwordsuite.environment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentManagerTest {

    private EnvironmentManager environmentManager;

    @BeforeEach
    void setUp() {
        environmentManager = new EnvironmentManager();
    }

    @Test
    void shouldStartWithNoEnvironments() {
        assertThat(environmentManager.hasEnvironments()).isFalse();
        assertThat(environmentManager.getEnvironmentCount()).isZero();
        assertThat(environmentManager.getActiveEnvironment()).isNull();
        assertThat(environmentManager.getActivePassword()).isEmpty();
    }

    @Test
    void shouldUpdateFromDialog() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "prod");

        assertThat(environmentManager.hasEnvironments()).isTrue();
        assertThat(environmentManager.getEnvironmentCount()).isEqualTo(2);
        assertThat(environmentManager.getActiveEnvironment()).isEqualTo("prod");
        assertThat(environmentManager.getEnvironmentNames()).containsExactlyInAnyOrder("dev", "prod");
    }

    @Test
    void shouldGetActivePasswordForSelectedEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "prod");

        assertThat(new String(environmentManager.getActivePassword())).isEqualTo("prod-password");
    }

    @Test
    void shouldGetFirstPasswordWhenNoActiveEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));

        environmentManager.updateFromDialog(environments, null);

        assertThat(environmentManager.getActivePassword()).isNotNull();
        assertThat(new String(environmentManager.getActivePassword())).isEqualTo("dev-password");
    }

    @Test
    void shouldCleanupPasswordsFromMemory() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        char[] password = "secret-password".toCharArray();
        environments.put("prod", new EnvironmentConfig(password));

        environmentManager.updateFromDialog(environments, "prod");
        assertThat(environmentManager.hasEnvironments()).isTrue();

        environmentManager.cleanup();

        assertThat(environmentManager.hasEnvironments()).isFalse();
        assertThat(environmentManager.getEnvironmentCount()).isZero();
        assertThat(environmentManager.getActiveEnvironment()).isNull();
    }

    @Test
    void shouldSetActiveEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "dev");
        assertThat(environmentManager.getActiveEnvironment()).isEqualTo("dev");

        environmentManager.setActiveEnvironment("prod");
        assertThat(environmentManager.getActiveEnvironment()).isEqualTo("prod");
    }

    @Test
    void shouldClearActiveEnvironmentForNoEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "dev");
        assertThat(environmentManager.getActiveEnvironment()).isEqualTo("dev");

        environmentManager.setActiveEnvironment("No Environment");
        assertThat(environmentManager.getActiveEnvironment()).isNull();
    }

    @Test
    void shouldCopyPasswordsNotReference() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        char[] originalPassword = "original".toCharArray();
        environments.put("test", new EnvironmentConfig(originalPassword));

        environmentManager.updateFromDialog(environments, "test");

        // Modify original password
        originalPassword[0] = 'X';

        // Password in manager should be unchanged
        assertThat(new String(environmentManager.getActivePassword())).isEqualTo("original");
    }

    @Test
    void shouldReturnEnvironmentNames() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("alpha", new EnvironmentConfig("a".toCharArray()));
        environments.put("beta", new EnvironmentConfig("b".toCharArray()));
        environments.put("gamma", new EnvironmentConfig("c".toCharArray()));

        environmentManager.updateFromDialog(environments, "alpha");

        List<String> names = environmentManager.getEnvironmentNames();
        assertThat(names).hasSize(3);
        assertThat(names).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void shouldReturnEnvironmentsCopy() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "dev");

        Map<String, EnvironmentConfig> copy = environmentManager.getEnvironmentsCopy();
        assertThat(copy).hasSize(2);
        assertThat(copy).containsKeys("dev", "prod");
        assertThat(new String(copy.get("dev").getPassword())).isEqualTo("dev-password");
        assertThat(new String(copy.get("prod").getPassword())).isEqualTo("prod-password");
    }

    @Test
    void shouldReturnIndependentCopyOfEnvironments() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("test", new EnvironmentConfig("test-password".toCharArray()));

        environmentManager.updateFromDialog(environments, "test");

        Map<String, EnvironmentConfig> copy = environmentManager.getEnvironmentsCopy();

        // Modify the copy
        copy.get("test").getPassword()[0] = 'X';

        // Original should be unchanged
        assertThat(new String(environmentManager.getActivePassword())).isEqualTo("test-password");
    }

    @Test
    void shouldStoreAndRetrieveFormatSettings() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("prod", new EnvironmentConfig("password".toCharArray(), "SEC[", "}", null));
        environments.put("dev", new EnvironmentConfig("password".toCharArray(), "ENC[", "]", null));

        environmentManager.updateFromDialog(environments, "prod");

        assertThat(environmentManager.getActiveFormatPrefix()).isEqualTo("SEC[");
        assertThat(environmentManager.getActiveFormatSuffix()).isEqualTo("}");

        environmentManager.setActiveEnvironment("dev");
        assertThat(environmentManager.getActiveFormatPrefix()).isEqualTo("ENC[");
        assertThat(environmentManager.getActiveFormatSuffix()).isEqualTo("]");
    }

    @Test
    void shouldReturnDefaultFormatWhenNoEnvironmentConfigured() {
        assertThat(environmentManager.getActiveFormatPrefix()).isEqualTo(EnvironmentConfig.DEFAULT_FORMAT_PREFIX);
        assertThat(environmentManager.getActiveFormatSuffix()).isEqualTo(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX);
    }

    @Test
    void shouldDetectEnvironmentByFilenamePattern() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments
                .put("prod", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null,
                        ".*-prod\\.properties"));
        environments
                .put("dev", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null,
                        ".*-dev\\.properties"));
        environmentManager.updateFromDialog(environments, "dev");

        assertThat(environmentManager.detectEnvironment("/home/user/app-prod.properties")).contains("prod");
        assertThat(environmentManager.detectEnvironment("/home/user/app-dev.properties")).contains("dev");
    }

    @Test
    void shouldReturnEmptyWhenNoPatternMatches() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments
                .put("prod", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null,
                        ".*-prod\\.properties"));
        environments.put("nopattern", new EnvironmentConfig("password12345678".toCharArray()));
        environmentManager.updateFromDialog(environments, "prod");

        assertThat(environmentManager.detectEnvironment("/home/user/app-staging.properties")).isEmpty();
    }

    @Test
    void shouldSkipInvalidPatternAndKeepDetecting() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        // "broken" sorts before "valid", so the invalid pattern is hit first
        environments
                .put("broken", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null, "[invalid"));
        environments
                .put("valid",
                        new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null, ".*\\.properties"));
        environmentManager.updateFromDialog(environments, "valid");

        assertThat(environmentManager.detectEnvironment("/home/user/app.properties")).contains("valid");
    }

    @Test
    void shouldDetectFirstEnvironmentAlphabeticallyWhenMultiplePatternsMatch() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("beta", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null, ".*"));
        environments.put("alpha", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null, ".*"));
        environmentManager.updateFromDialog(environments, "alpha");

        assertThat(environmentManager.detectEnvironment("/any/file.properties")).contains("alpha");
    }

    @Test
    void shouldPreservePatternInCopies() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments
                .put("prod", new EnvironmentConfig("password12345678".toCharArray(), "ENC[", "]", null,
                        ".*-prod\\.properties"));
        environmentManager.updateFromDialog(environments, "prod");

        Map<String, EnvironmentConfig> copy = environmentManager.getEnvironmentsCopy();
        assertThat(copy.get("prod").getPattern()).isEqualTo(".*-prod\\.properties");
    }
}
