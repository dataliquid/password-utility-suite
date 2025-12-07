package com.dataliquid.passwordsuite.ui.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordManagerTest {

    private PasswordManager passwordManager;

    @BeforeEach
    void setUp() {
        passwordManager = new PasswordManager();
    }

    @Test
    void shouldStartWithNoEnvironments() {
        assertThat(passwordManager.hasEnvironments()).isFalse();
        assertThat(passwordManager.getEnvironmentCount()).isZero();
        assertThat(passwordManager.getActiveEnvironment()).isNull();
        assertThat(passwordManager.getActivePassword()).isEmpty();
    }

    @Test
    void shouldUpdateFromDialog() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "prod");

        assertThat(passwordManager.hasEnvironments()).isTrue();
        assertThat(passwordManager.getEnvironmentCount()).isEqualTo(2);
        assertThat(passwordManager.getActiveEnvironment()).isEqualTo("prod");
        assertThat(passwordManager.getEnvironmentNames()).containsExactlyInAnyOrder("dev", "prod");
    }

    @Test
    void shouldGetActivePasswordForSelectedEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "prod");

        assertThat(new String(passwordManager.getActivePassword())).isEqualTo("prod-password");
    }

    @Test
    void shouldGetFirstPasswordWhenNoActiveEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));

        passwordManager.updateFromDialog(environments, null);

        assertThat(passwordManager.getActivePassword()).isNotNull();
        assertThat(new String(passwordManager.getActivePassword())).isEqualTo("dev-password");
    }

    @Test
    void shouldCleanupPasswordsFromMemory() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        char[] password = "secret-password".toCharArray();
        environments.put("prod", new EnvironmentConfig(password));

        passwordManager.updateFromDialog(environments, "prod");
        assertThat(passwordManager.hasEnvironments()).isTrue();

        passwordManager.cleanup();

        assertThat(passwordManager.hasEnvironments()).isFalse();
        assertThat(passwordManager.getEnvironmentCount()).isZero();
        assertThat(passwordManager.getActiveEnvironment()).isNull();
    }

    @Test
    void shouldSetActiveEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "dev");
        assertThat(passwordManager.getActiveEnvironment()).isEqualTo("dev");

        passwordManager.setActiveEnvironment("prod");
        assertThat(passwordManager.getActiveEnvironment()).isEqualTo("prod");
    }

    @Test
    void shouldClearActiveEnvironmentForNoEnvironment() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "dev");
        assertThat(passwordManager.getActiveEnvironment()).isEqualTo("dev");

        passwordManager.setActiveEnvironment("No Environment");
        assertThat(passwordManager.getActiveEnvironment()).isNull();
    }

    @Test
    void shouldCopyPasswordsNotReference() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        char[] originalPassword = "original".toCharArray();
        environments.put("test", new EnvironmentConfig(originalPassword));

        passwordManager.updateFromDialog(environments, "test");

        // Modify original password
        originalPassword[0] = 'X';

        // Password in manager should be unchanged
        assertThat(new String(passwordManager.getActivePassword())).isEqualTo("original");
    }

    @Test
    void shouldReturnEnvironmentNames() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("alpha", new EnvironmentConfig("a".toCharArray()));
        environments.put("beta", new EnvironmentConfig("b".toCharArray()));
        environments.put("gamma", new EnvironmentConfig("c".toCharArray()));

        passwordManager.updateFromDialog(environments, "alpha");

        List<String> names = passwordManager.getEnvironmentNames();
        assertThat(names).hasSize(3);
        assertThat(names).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    @Test
    void shouldReturnEnvironmentsCopy() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("dev", new EnvironmentConfig("dev-password".toCharArray()));
        environments.put("prod", new EnvironmentConfig("prod-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "dev");

        Map<String, EnvironmentConfig> copy = passwordManager.getEnvironmentsCopy();
        assertThat(copy).hasSize(2);
        assertThat(copy).containsKeys("dev", "prod");
        assertThat(new String(copy.get("dev").getPassword())).isEqualTo("dev-password");
        assertThat(new String(copy.get("prod").getPassword())).isEqualTo("prod-password");
    }

    @Test
    void shouldReturnIndependentCopyOfEnvironments() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("test", new EnvironmentConfig("test-password".toCharArray()));

        passwordManager.updateFromDialog(environments, "test");

        Map<String, EnvironmentConfig> copy = passwordManager.getEnvironmentsCopy();

        // Modify the copy
        copy.get("test").getPassword()[0] = 'X';

        // Original should be unchanged
        assertThat(new String(passwordManager.getActivePassword())).isEqualTo("test-password");
    }

    @Test
    void shouldStoreAndRetrieveFormatSettings() {
        Map<String, EnvironmentConfig> environments = new HashMap<>();
        environments.put("prod", new EnvironmentConfig("password".toCharArray(), "SEC[", "}"));
        environments.put("dev", new EnvironmentConfig("password".toCharArray(), "ENC[", "]"));

        passwordManager.updateFromDialog(environments, "prod");

        assertThat(passwordManager.getActiveFormatPrefix()).isEqualTo("SEC[");
        assertThat(passwordManager.getActiveFormatSuffix()).isEqualTo("}");

        passwordManager.setActiveEnvironment("dev");
        assertThat(passwordManager.getActiveFormatPrefix()).isEqualTo("ENC[");
        assertThat(passwordManager.getActiveFormatSuffix()).isEqualTo("]");
    }

    @Test
    void shouldReturnDefaultFormatWhenNoEnvironmentConfigured() {
        assertThat(passwordManager.getActiveFormatPrefix()).isEqualTo(EnvironmentConfig.DEFAULT_FORMAT_PREFIX);
        assertThat(passwordManager.getActiveFormatSuffix()).isEqualTo(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX);
    }
}
