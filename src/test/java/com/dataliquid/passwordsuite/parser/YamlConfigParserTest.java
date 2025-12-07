package com.dataliquid.passwordsuite.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigTree;

class YamlConfigParserTest {

    private final YamlConfigParser parser = new YamlConfigParser();

    @Test
    void shouldParseFlatYaml() throws ParseException {
        String yaml = "host: localhost\n" + "port: 5432\n" + "username: admin";

        ConfigTree tree = parser.parse(yaml);

        assertThat(tree.getFormat()).isEqualTo(ConfigFormat.YAML);
        assertThat(tree.getEntryCount()).isEqualTo(3);
    }

    @Test
    void shouldParseNestedYaml() throws ParseException {
        String yaml = "database:\n" + "  host: localhost\n" + "  port: 5432";

        ConfigTree tree = parser.parse(yaml);

        assertThat(tree.getEntryCount()).isEqualTo(2);
        assertThat(tree.findByPath("database.host")).isPresent();
        assertThat(tree.findByPath("database.port")).isPresent();
    }

    @Test
    void shouldDetectEncryptedValues() throws ParseException {
        String yaml = "database:\n" + "  password: ENC[base64data]";

        ConfigTree tree = parser.parse(yaml);

        ConfigEntry entry = (ConfigEntry) tree.findByPath("database.password").orElseThrow();
        assertThat(entry.isEncrypted()).isTrue();
        assertThat(entry.getValue()).isEqualTo("ENC[base64data]");
    }

    @Test
    void shouldAutoMarkEncryptedValuesForEncryption() throws ParseException {
        String yaml = "database:\n" + "  password: ENC[base64data]\n" + "  host: localhost";

        ConfigTree tree = parser.parse(yaml);

        ConfigEntry passwordEntry = (ConfigEntry) tree.findByPath("database.password").orElseThrow();
        assertThat(passwordEntry.isMarkedForEncryption()).isTrue();

        ConfigEntry hostEntry = (ConfigEntry) tree.findByPath("database.host").orElseThrow();
        assertThat(hostEntry.isMarkedForEncryption()).isFalse();
    }

    @Test
    void shouldThrowExceptionForEmptyContent() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForNullContent() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForInvalidYaml() {
        String invalidYaml = "this is not: valid: yaml:";

        assertThatThrownBy(() -> parser.parse(invalidYaml))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Failed to parse YAML");
    }

    @Test
    void shouldGetSupportedFormat() {
        assertThat(parser.getSupportedFormat()).isEqualTo(ConfigFormat.YAML);
    }

    @Test
    void shouldHandleDeepNesting() throws ParseException {
        String yaml = "level1:\n" + "  level2:\n" + "    level3:\n" + "      value: deep";

        ConfigTree tree = parser.parse(yaml);

        assertThat(tree.findByPath("level1.level2.level3.value")).isPresent();
        ConfigEntry entry = (ConfigEntry) tree.findByPath("level1.level2.level3.value").orElseThrow();
        assertThat(entry.getValue()).isEqualTo("deep");
    }

    @Test
    void shouldParseComplexNestedYamlWithLineNumbers() throws ParseException {
        // Simulates structure similar to test-credentials.yml
        String yaml = """
                # AWS Credentials
                aws:
                  access_key_id: AKIAIOSFODNN7EXAMPLE
                  secret_access_key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
                  region: eu-central-1

                # Azure Credentials
                azure:
                  tenant_id: 12345678-1234-1234-1234-123456789012
                  client_id: 87654321-4321-4321-4321-210987654321
                  client_secret: azure_client_secret_xyz123

                # Third-party APIs
                external_apis:
                  stripe:
                    public_key: pk_test_51234567890
                    secret_key: sk_test_51234567890

                  twilio:
                    account_sid: AC1234567890abcdef
                    auth_token: twilio_auth_token_xyz
                    phone_number: "+491234567890"

                  github:
                    personal_access_token: ghp_1234567890abcdefghijklmnop
                    oauth_client_id: Iv1.1234567890abcdef
                    oauth_client_secret: 1234567890abcdef1234567890abcdef12345678
                """;

        ConfigTree tree = parser.parse(yaml);

        // Verify all entries exist
        assertThat(tree.findByPath("aws.access_key_id")).isPresent();
        assertThat(tree.findByPath("aws.secret_access_key")).isPresent();
        assertThat(tree.findByPath("aws.region")).isPresent();

        assertThat(tree.findByPath("azure.tenant_id")).isPresent();
        assertThat(tree.findByPath("azure.client_id")).isPresent();
        assertThat(tree.findByPath("azure.client_secret")).isPresent();

        assertThat(tree.findByPath("external_apis.stripe.public_key")).isPresent();
        assertThat(tree.findByPath("external_apis.stripe.secret_key")).isPresent();

        assertThat(tree.findByPath("external_apis.twilio.account_sid")).isPresent();
        assertThat(tree.findByPath("external_apis.twilio.auth_token")).isPresent();
        assertThat(tree.findByPath("external_apis.twilio.phone_number")).isPresent();

        assertThat(tree.findByPath("external_apis.github.personal_access_token")).isPresent();
        assertThat(tree.findByPath("external_apis.github.oauth_client_id")).isPresent();
        assertThat(tree.findByPath("external_apis.github.oauth_client_secret")).isPresent();

        // Verify correct values
        ConfigEntry awsAccessKey = (ConfigEntry) tree.findByPath("aws.access_key_id").orElseThrow();
        assertThat(awsAccessKey.getValue()).isEqualTo("AKIAIOSFODNN7EXAMPLE");

        ConfigEntry stripePublicKey = (ConfigEntry) tree.findByPath("external_apis.stripe.public_key").orElseThrow();
        assertThat(stripePublicKey.getValue()).isEqualTo("pk_test_51234567890");

        // Verify line numbers are tracked correctly
        // Line 2: aws.access_key_id
        assertThat(awsAccessKey.getLineNumber()).isEqualTo(2);

        // Line 3: aws.secret_access_key
        ConfigEntry awsSecretKey = (ConfigEntry) tree.findByPath("aws.secret_access_key").orElseThrow();
        assertThat(awsSecretKey.getLineNumber()).isEqualTo(3);

        // Line 4: aws.region
        ConfigEntry awsRegion = (ConfigEntry) tree.findByPath("aws.region").orElseThrow();
        assertThat(awsRegion.getLineNumber()).isEqualTo(4);

        // Line 8: azure.tenant_id
        ConfigEntry azureTenantId = (ConfigEntry) tree.findByPath("azure.tenant_id").orElseThrow();
        assertThat(azureTenantId.getLineNumber()).isEqualTo(8);

        // Line 15: external_apis.stripe.public_key
        assertThat(stripePublicKey.getLineNumber()).isEqualTo(15);

        // Line 16: external_apis.stripe.secret_key
        ConfigEntry stripeSecretKey = (ConfigEntry) tree.findByPath("external_apis.stripe.secret_key").orElseThrow();
        assertThat(stripeSecretKey.getLineNumber()).isEqualTo(16);

        // Line 19: external_apis.twilio.account_sid
        ConfigEntry twilioAccountSid = (ConfigEntry) tree.findByPath("external_apis.twilio.account_sid").orElseThrow();
        assertThat(twilioAccountSid.getLineNumber()).isEqualTo(19);

        // Line 24: external_apis.github.personal_access_token
        ConfigEntry githubToken = (ConfigEntry) tree
                .findByPath("external_apis.github.personal_access_token")
                .orElseThrow();
        assertThat(githubToken.getLineNumber()).isEqualTo(24);

        // Line 26: external_apis.github.oauth_client_secret (tests duplicate "secret"
        // keys)
        ConfigEntry githubSecret = (ConfigEntry) tree
                .findByPath("external_apis.github.oauth_client_secret")
                .orElseThrow();
        assertThat(githubSecret.getLineNumber()).isEqualTo(26);
    }

    @Test
    void shouldParseActualTestCredentialsFile() throws ParseException {
        // Test with actual test-credentials.yml structure including multi-line values
        String yaml = """
                # AWS Credentials
                aws:
                  access_key_id: AKIAIOSFODNN7EXAMPLE
                  secret_access_key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
                  region: eu-central-1

                # Azure Credentials
                azure:
                  tenant_id: 12345678-1234-1234-1234-123456789012
                  client_id: 87654321-4321-4321-4321-210987654321
                  client_secret: azure_client_secret_xyz123

                # Google Cloud
                gcp:
                  project_id: my-project-12345
                  service_account_key: |
                    {
                      "type": "service_account",
                      "project_id": "my-project-12345",
                      "private_key_id": "key123",
                      "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASC...\\n-----END PRIVATE KEY-----\\n"
                    }

                # Third-party APIs
                external_apis:
                  stripe:
                    public_key: pk_test_51234567890
                    secret_key: sk_test_51234567890

                  twilio:
                    account_sid: AC1234567890abcdef
                    auth_token: twilio_auth_token_xyz
                    phone_number: "+491234567890"

                  github:
                    personal_access_token: ghp_1234567890abcdefghijklmnop
                    oauth_client_id: Iv1.1234567890abcdef
                    oauth_client_secret: 1234567890abcdef1234567890abcdef12345678
                """;

        ConfigTree tree = parser.parse(yaml);

        // Verify all top-level groups exist
        assertThat(tree.getRoot().findChildByKey("aws")).isPresent();
        assertThat(tree.getRoot().findChildByKey("azure")).isPresent();
        assertThat(tree.getRoot().findChildByKey("gcp")).isPresent();
        assertThat(tree.getRoot().findChildByKey("external_apis")).isPresent();

        // Verify all nested entries exist
        assertThat(tree.findByPath("aws.access_key_id")).as("aws.access_key_id should exist").isPresent();
        assertThat(tree.findByPath("aws.secret_access_key")).as("aws.secret_access_key should exist").isPresent();
        assertThat(tree.findByPath("aws.region")).as("aws.region should exist").isPresent();

        assertThat(tree.findByPath("azure.tenant_id")).as("azure.tenant_id should exist").isPresent();
        assertThat(tree.findByPath("azure.client_id")).as("azure.client_id should exist").isPresent();
        assertThat(tree.findByPath("azure.client_secret")).as("azure.client_secret should exist").isPresent();

        assertThat(tree.findByPath("gcp.project_id")).as("gcp.project_id should exist").isPresent();
        assertThat(tree.findByPath("gcp.service_account_key")).as("gcp.service_account_key should exist").isPresent();

        assertThat(tree.findByPath("external_apis.stripe.public_key"))
                .as("external_apis.stripe.public_key should exist")
                .isPresent();
        assertThat(tree.findByPath("external_apis.stripe.secret_key"))
                .as("external_apis.stripe.secret_key should exist")
                .isPresent();

        assertThat(tree.findByPath("external_apis.twilio.account_sid"))
                .as("external_apis.twilio.account_sid should exist")
                .isPresent();
        assertThat(tree.findByPath("external_apis.twilio.auth_token"))
                .as("external_apis.twilio.auth_token should exist")
                .isPresent();
        assertThat(tree.findByPath("external_apis.twilio.phone_number"))
                .as("external_apis.twilio.phone_number should exist")
                .isPresent();

        assertThat(tree.findByPath("external_apis.github.personal_access_token"))
                .as("external_apis.github.personal_access_token should exist")
                .isPresent();
        assertThat(tree.findByPath("external_apis.github.oauth_client_id"))
                .as("external_apis.github.oauth_client_id should exist")
                .isPresent();
        assertThat(tree.findByPath("external_apis.github.oauth_client_secret"))
                .as("external_apis.github.oauth_client_secret should exist")
                .isPresent();

        // Verify total entry count (should be 16)
        assertThat(tree.getEntryCount()).as("Total entry count should be 16").isEqualTo(16);

        // Verify multi-line value handling
        ConfigEntry gcpKey = (ConfigEntry) tree.findByPath("gcp.service_account_key").orElseThrow();
        assertThat(gcpKey.getValue()).contains("type");
        assertThat(gcpKey.getValue()).contains("service_account");
    }

    @Test
    void shouldParseTestApplicationYamlStructure() throws ParseException {
        // Test the actual test-application.yaml structure with multiple sibling groups
        // at same level
        String yaml = """
                database:
                  primary:
                    host: db.production.example.com
                    port: 5432
                    username: prod_user
                    password: prodPassword789

                  secondary:
                    host: db-replica.production.example.com
                    port: 5432
                    username: readonly_user
                    password: readonlyPass456

                redis:
                  host: redis.example.com
                  port: 6379
                  password: redisSecret123

                security:
                  jwt:
                    secret: jwt_secret_key_12345
                    expiration: 3600

                  oauth:
                    client-id: oauth-client-123
                    client-secret: oauth-secret-xyz789

                services:
                  payment:
                    api-key: pk_live_abcdef123456
                    webhook-secret: whsec_123456789

                  email:
                    api-key: sendgrid-api-key-xyz
                    from-address: noreply@example.com
                """;

        ConfigTree tree = parser.parse(yaml);

        // Test database.primary entries
        assertThat(tree.findByPath("database.primary.host")).as("database.primary.host should exist").isPresent();
        assertThat(tree.findByPath("database.primary.port")).as("database.primary.port should exist").isPresent();
        assertThat(tree.findByPath("database.primary.username"))
                .as("database.primary.username should exist")
                .isPresent();
        assertThat(tree.findByPath("database.primary.password"))
                .as("database.primary.password should exist")
                .isPresent();

        // Test database.secondary entries (this is where the problem might be)
        assertThat(tree.findByPath("database.secondary.host")).as("database.secondary.host should exist").isPresent();
        assertThat(tree.findByPath("database.secondary.port")).as("database.secondary.port should exist").isPresent();
        assertThat(tree.findByPath("database.secondary.username"))
                .as("database.secondary.username should exist")
                .isPresent();
        assertThat(tree.findByPath("database.secondary.password"))
                .as("database.secondary.password should exist")
                .isPresent();

        // Test redis entries
        assertThat(tree.findByPath("redis.host")).as("redis.host should exist").isPresent();
        assertThat(tree.findByPath("redis.port")).as("redis.port should exist").isPresent();
        assertThat(tree.findByPath("redis.password")).as("redis.password should exist").isPresent();

        // Test security.jwt entries
        assertThat(tree.findByPath("security.jwt.secret")).as("security.jwt.secret should exist").isPresent();
        assertThat(tree.findByPath("security.jwt.expiration")).as("security.jwt.expiration should exist").isPresent();

        // Test security.oauth entries
        assertThat(tree.findByPath("security.oauth.client-id")).as("security.oauth.client-id should exist").isPresent();
        assertThat(tree.findByPath("security.oauth.client-secret"))
                .as("security.oauth.client-secret should exist")
                .isPresent();

        // Test services.payment entries
        assertThat(tree.findByPath("services.payment.api-key")).as("services.payment.api-key should exist").isPresent();
        assertThat(tree.findByPath("services.payment.webhook-secret"))
                .as("services.payment.webhook-secret should exist")
                .isPresent();

        // Test services.email entries
        assertThat(tree.findByPath("services.email.api-key")).as("services.email.api-key should exist").isPresent();
        assertThat(tree.findByPath("services.email.from-address"))
                .as("services.email.from-address should exist")
                .isPresent();

        // Verify values
        ConfigEntry secondaryHost = (ConfigEntry) tree.findByPath("database.secondary.host").orElseThrow();
        assertThat(secondaryHost.getValue()).isEqualTo("db-replica.production.example.com");

        ConfigEntry redisPassword = (ConfigEntry) tree.findByPath("redis.password").orElseThrow();
        assertThat(redisPassword.getValue()).isEqualTo("redisSecret123");
    }

    @Test
    void shouldHandleDuplicateKeysWithSameValue() throws ParseException {
        // Test the specific case where duplicate keys have the same value
        // This was causing the issue where encryption replaced the wrong line
        // Line numbers: 0=database, 1=primary, 2=host, 3=port, 4=database(FIRST),
        // 5=secondary, 6=host, 7=port, 8=database(SECOND, same value!)
        String yaml = """
                database:
                  primary:
                    host: db1.example.com
                    port: 5432
                    database: app_production
                  secondary:
                    host: db2.example.com
                    port: 5432
                    database: app_production
                """;

        ConfigTree tree = parser.parse(yaml);

        // Verify both entries exist
        ConfigEntry primaryDb = (ConfigEntry) tree.findByPath("database.primary.database").orElseThrow();
        ConfigEntry secondaryDb = (ConfigEntry) tree.findByPath("database.secondary.database").orElseThrow();

        // Both should have the same value
        assertThat(primaryDb.getValue()).isEqualTo("app_production");
        assertThat(secondaryDb.getValue()).isEqualTo("app_production");

        // CRITICAL: They should have DIFFERENT line numbers
        assertThat(primaryDb.getLineNumber()).as("primary.database should be at line 4").isEqualTo(4);
        assertThat(secondaryDb.getLineNumber()).as("secondary.database should be at line 8").isEqualTo(8);

        // Verify no collision - line numbers should be different
        assertThat(primaryDb.getLineNumber())
                .as("Line numbers should not collide")
                .isNotEqualTo(secondaryDb.getLineNumber());
    }
}
